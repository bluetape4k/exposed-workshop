package exposed.examples.connection

import exposed.shared.tests.AbstractExposedTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.fail
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.SQLTransientException

/**
 * 데이터베이스 연결 시도 실패 시 재시도(maxAttempts) 동작을 검증하는 테스트 클래스.
 * [DatabaseConfig.defaultMaxAttempts]와 트랜잭션 블록 내 [maxAttempts] 설정 간의
 * 우선순위 및 재시도 횟수를 확인합니다.
 */
class Ex03_ConnectionTimeout: AbstractExposedTest() {

    companion object: KLoggingChannel()

    /**
     * Connect 수행 시 [GetConnectException]을 발생 시키는 DataSource입니다.
     */
    private class ExceptionOnGetConnectionDataSource: DataSourceStub() {
        var connectCount = 0

        override fun getConnection(): Connection {
            connectCount++
            throw GetConnectException()
        }
    }

    private class GetConnectException: SQLTransientException()

    /**
     * 연결을 시도하면, [GetConnectException]이 발생합니다.
     * `maxAttempts` 수만큼 재시도 합니다.
     */
    @Test
    fun `connect fail causes repeated connect attempts`() {
        val datasource = ExceptionOnGetConnectionDataSource()
        val db = Database.connect(datasource = datasource)

        try {
            transaction(db = db, transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                maxAttempts = 3
                exec("SELECT 1;")
                // NO OP
            }
            fail("Should have thrown ${GetConnectException::class.simpleName}")
        } catch (e: ExposedSQLException) {
            e.cause shouldBeInstanceOf GetConnectException::class
            datasource.connectCount shouldBeEqualTo 3
        } finally {
            TransactionManager.closeAndUnregister(db)
        }
    }

    /**
     * transaction 블록에서 설정한 `maxAttempts` 가 [DatabaseConfig]의 기본값을 덮어씁니다.
     * `maxAttempts` 는 예외 발생 시, 재시도 횟수를 설정합니다.
     */
    @Test
    fun `transaction repeatition with defaults`() {
        val datasource = ExceptionOnGetConnectionDataSource()
        val db = Database.connect(
            datasource = datasource,
            databaseConfig = DatabaseConfig {
                defaultMaxAttempts = 3
            }
        )

        try {
            try {
                // 별도 속성이 없으면 transaction block은 기본 DatabaseConfig 값을 사용해야 합니다.
                transaction(db = db, transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                    exec("SELECT 1;")
                }
                fail("Should have thrown ${GetConnectException::class.simpleName}")
            } catch (e: ExposedSQLException) {
                e.cause shouldBeInstanceOf GetConnectException::class
                datasource.connectCount shouldBeEqualTo 3
            }

            datasource.connectCount = 0

            try {
                // transaction block에서 지정한 속성은 기본 DatabaseConfig 값을 재정의해야 합니다.
                transaction(db = db, transactionIsolation = Connection.TRANSACTION_SERIALIZABLE) {
                    maxAttempts = 5
                    exec("SELECT 1;")
                }
                fail("Should have thrown ${GetConnectException::class.simpleName}")
            } catch (e: ExposedSQLException) {
                e.cause shouldBeInstanceOf GetConnectException::class
                datasource.connectCount shouldBeEqualTo 5
            }
        } finally {
            TransactionManager.closeAndUnregister(db)
        }
    }
}
