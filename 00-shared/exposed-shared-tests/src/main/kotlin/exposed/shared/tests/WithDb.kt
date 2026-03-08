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

internal val registeredOnShutdown = ConcurrentHashMap.newKeySet<TestDB>()
internal val testDbSemaphores = ConcurrentHashMap<TestDB, Semaphore>()

var currentTestDB by nullableTransactionScope<TestDB>()

/**
 * 트랜잭션 커밋 시 현재 테스트 DB 정보를 트랜잭션 스코프에 유지하는 인터셉터.
 */
object CurrentTestDBInterceptor: StatementInterceptor {
    /**
     * 커밋 후에도 [TestDB] 타입의 사용자 데이터만 유지합니다.
     *
     * @param userData 현재 트랜잭션에 저장된 사용자 데이터 맵
     * @return [TestDB] 값만 필터링한 맵
     */
    override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
        return userData.filterValues { it is TestDB }
    }
}

/**
 * 지정된 [testDB]에 대해 트랜잭션 블록을 실행합니다.
 *
 * 세마포어를 통해 동일 DB에 대한 동시 접근을 방지하며, 최초 실행 시 DB 연결을 초기화합니다.
 *
 * @param testDB 사용할 테스트 데이터베이스
 * @param configure 데이터베이스 설정 빌더 람다 (선택사항)
 * @param statement 트랜잭션 내에서 실행할 블록
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
