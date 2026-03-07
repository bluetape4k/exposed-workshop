package exposed.shared.tests

import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import org.jetbrains.exposed.v1.core.transactions.nullableTransactionScope
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

/**
 * JVM 종료 시 정리(cleanup)가 등록된 [TestDB] 인스턴스의 집합.
 *
 * 중복 등록을 방지하기 위해 스레드 안전한 [ConcurrentHashMap] 기반 Set으로 관리됩니다.
 */
internal val registeredOnShutdown = ConcurrentHashMap.newKeySet<TestDB>()

/**
 * 각 [TestDB]에 대한 동시 접근을 제어하는 세마포어 맵.
 *
 * 동일한 데이터베이스에 대한 병렬 테스트 실행을 방지하여 테스트 격리를 보장합니다.
 */
internal val testDbSemaphores = ConcurrentHashMap<TestDB, Semaphore>()

/**
 * 현재 트랜잭션 스코프에서 사용 중인 [TestDB]를 나타내는 변수.
 *
 * 트랜잭션 컨텍스트에 바인딩되어 있으며, 트랜잭션 외부에서는 `null`입니다.
 */
var currentTestDB by nullableTransactionScope<TestDB>()

/**
 * 트랜잭션 커밋 시 [TestDB] 관련 사용자 데이터를 유지하는 인터셉터.
 *
 * 커밋 후에도 현재 테스트 DB 정보가 트랜잭션 스토어에 남아있도록 합니다.
 */
object CurrentTestDBInterceptor: StatementInterceptor {
    override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
        return userData.filterValues { it is TestDB }
    }
}

/**
 * 지정된 [TestDB]에 연결하여 동기 트랜잭션 블록을 실행합니다.
 *
 * - 세마포어를 사용하여 동일 데이터베이스에 대한 동시 접근을 직렬화합니다.
 * - 최초 호출 시 데이터베이스에 연결하고 JVM 종료 시 정리 훅을 등록합니다.
 * - 새로운 설정(`configure`)이 제공된 경우, 테스트 완료 후 이전 설정으로 복원합니다.
 *
 * @param testDB 테스트에 사용할 데이터베이스 종류
 * @param configure 데이터베이스 설정을 커스터마이즈하는 빌더 람다. `null`이면 기본 설정 사용
 * @param statement 트랜잭션 내에서 실행할 코드 블록. [TestDB]를 인자로 받습니다
 */
fun withDb(
    testDB: TestDB,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: JdbcTransaction.(TestDB) -> Unit,
) {
    logger.info { "Running `withDb` for $testDB" }
    val semaphore = testDbSemaphores.computeIfAbsent(testDB) { Semaphore(1, true) }
    semaphore.acquire()
    try {
        val unregistered = testDB !in registeredOnShutdown
        val newConfiguration = configure != null && !unregistered

        if (unregistered) {
            Runtimex.addShutdownHook {
                testDB.afterTestFinished()
                registeredOnShutdown.remove(testDB)
            }
            registeredOnShutdown += testDB
            testDB.db = testDB.connect(configure ?: {})
        }

        val registeredDb = testDB.db
        try {
            if (newConfiguration) {
                testDB.db = testDB.connect(configure)
            }
            val database = testDB.db!!
            transaction(
                transactionIsolation = database.transactionManager.defaultIsolationLevel,
                db = database,
            ) {
                maxAttempts = 1
                registerInterceptor(CurrentTestDBInterceptor)  // interceptor 를 통해 다양한 작업을 할 수 있다
                currentTestDB = testDB
                statement(testDB)
            }
        } finally {
            // revert any new configuration to not be carried over to the next test in suite
            if (configure != null) {
                testDB.db = registeredDb
            }
        }
    } finally {
        semaphore.release()
    }
}
