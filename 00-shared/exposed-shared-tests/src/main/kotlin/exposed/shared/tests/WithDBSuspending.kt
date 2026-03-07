package exposed.shared.tests

import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import java.util.concurrent.Semaphore
import kotlin.coroutines.CoroutineContext

/**
 * 코루틴 환경에서 [TestDB]의 세마포어를 비동기적으로 획득합니다.
 *
 * [Dispatchers.IO]를 사용하여 블로킹 세마포어 획득을 IO 스레드 풀에서 실행합니다.
 *
 * @param testDB 세마포어를 획득할 데이터베이스 종류
 */
private suspend fun acquireSemaphoreSuspending(testDB: TestDB) =
    withContext(Dispatchers.IO) {
        testDbSemaphores.computeIfAbsent(testDB) { Semaphore(1, true) }.acquire()
    }

/**
 * 지정된 [TestDB]에 연결하여 코루틴 기반 서스펜딩 트랜잭션 블록을 실행합니다.
 *
 * - 세마포어를 사용하여 동일 데이터베이스에 대한 동시 접근을 직렬화합니다.
 * - 최초 호출 시 데이터베이스에 연결하고 JVM 종료 시 정리 훅을 등록합니다.
 * - `@ParameterizedTest`와 코루틴을 함께 사용할 때 발생할 수 있는 TestDB 혼용 문제를 방지합니다.
 * - 새로운 설정(`configure`)이 제공된 경우, 테스트 완료 후 이전 설정으로 복원합니다.
 *
 * @param testDB 테스트에 사용할 데이터베이스 종류
 * @param context 트랜잭션을 실행할 코루틴 컨텍스트. 기본값은 [Dispatchers.IO]
 * @param configure 데이터베이스 설정을 커스터마이즈하는 빌더 람다. `null`이면 기본 설정 사용
 * @param statement 서스펜딩 트랜잭션 내에서 실행할 코드 블록. [TestDB]를 인자로 받습니다
 * @see withDb
 */
@Suppress("DEPRECATION")
suspend fun withDbSuspending(
    testDB: TestDB,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend JdbcTransaction.(TestDB) -> Unit,
) {
    logger.info { "Running withDbSuspending for $testDB" }
    acquireSemaphoreSuspending(testDB)
    try {
        val unregistered = testDB !in registeredOnShutdown
        val newConfiguration = configure != null && !unregistered

        if (unregistered) {
            testDB.beforeConnection()
            Runtimex.addShutdownHook {
                testDB.afterTestFinished()
                registeredOnShutdown.remove(testDB)
            }
            registeredOnShutdown += testDB
            testDB.db = testDB.connect(configure ?: {})
        }

        val registeredDb = testDB.db!!
        try {
            // NOTE: 코루틴과 @ParameterizedTest 를 동시에 사용할 때, TestDB가 꼬일 때가 있다. 그래서 매번 connect 를 수행하도록 수정
            if (newConfiguration) {
                testDB.db = testDB.connect(configure)
            }
            val database = testDB.db!!
            newSuspendedTransaction(
                context = context,
                db = database,
                transactionIsolation = database.transactionManager.defaultIsolationLevel,
            ) {
                maxAttempts = 1
                registerInterceptor(CurrentTestDBInterceptor)
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
        testDbSemaphores.getValue(testDB).release()
    }
}

/**
 * [withDbSuspending]의 이전 버전 호환을 위한 deprecated 함수.
 *
 * 새로운 코드에서는 [withDbSuspending]을 사용하세요.
 *
 * @param testDB 테스트에 사용할 데이터베이스 종류
 * @param context 트랜잭션을 실행할 코루틴 컨텍스트. 기본값은 [Dispatchers.IO]
 * @param configure 데이터베이스 설정을 커스터마이즈하는 빌더 람다. `null`이면 기본 설정 사용
 * @param statement 서스펜딩 트랜잭션 내에서 실행할 코드 블록
 * @see withDbSuspending
 */
@Deprecated(
    message = "Use withDbSuspending() instead.",
    replaceWith = ReplaceWith(
        "withDbSuspending(testDB, context, configure, statement)",
        "io.bluetape4k.exposed.tests.withDbSuspending"
    )
)
suspend fun withSuspendedDb(
    testDB: TestDB,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend JdbcTransaction.(TestDB) -> Unit,
) {
    withDbSuspending(testDB, context, configure, statement)
}
