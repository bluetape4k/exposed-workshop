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

private suspend fun acquireSemaphoreSuspending(testDB: TestDB) =
    withContext(Dispatchers.IO) {
        testDbSemaphores.computeIfAbsent(testDB) { Semaphore(1, true) }.acquire()
    }

/**
 * 코루틴 환경에서 지정된 [testDB]에 대해 서스펜딩 트랜잭션 블록을 실행합니다.
 *
 * 세마포어를 통해 동일 DB에 대한 동시 접근을 방지하며, 최초 실행 시 DB 연결을 초기화합니다.
 *
 * @param testDB 사용할 테스트 데이터베이스
 * @param context 트랜잭션에 사용할 코루틴 컨텍스트 (기본값: [Dispatchers.IO])
 * @param configure 데이터베이스 설정 빌더 람다 (선택사항)
 * @param statement 트랜잭션 내에서 실행할 서스펜딩 블록
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

        val registeredDb = testDB.db
        try {
            // NOTE: 코루틴과 @ParameterizedTest 를 동시에 사용할 때, TestDB가 꼬일 때가 있다. 그래서 매번 connect 를 수행하도록 수정
            if (newConfiguration) {
                testDB.db = testDB.connect(configure)
            }
            val database = requireNotNull(testDB.db) { "DB 연결 초기화 실패: ${testDB.name}" }
            newSuspendedTransaction(
                context = context,
                db = database,
                transactionIsolation = database.transactionManager.defaultIsolationLevel
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
 * [withDbSuspending]의 구 버전 별칭입니다.
 *
 * @param testDB 사용할 테스트 데이터베이스
 * @param context 트랜잭션에 사용할 코루틴 컨텍스트 (기본값: [Dispatchers.IO])
 * @param configure 데이터베이스 설정 빌더 람다 (선택사항)
 * @param statement 트랜잭션 내에서 실행할 서스펜딩 블록
 */
@Deprecated(
    message = "Use withDbSuspending() instead.",
    replaceWith =
        ReplaceWith(
            "withDbSuspending(testDB, context, configure, statement)",
            "exposed.shared.tests.withDbSuspending"
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
