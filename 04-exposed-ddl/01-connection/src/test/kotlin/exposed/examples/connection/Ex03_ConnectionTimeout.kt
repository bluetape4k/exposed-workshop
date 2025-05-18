package exposed.examples.connection

import exposed.shared.tests.AbstractExposedTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.SQLTransientException

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
            transaction(Connection.TRANSACTION_SERIALIZABLE, db = db) {
                maxAttempts = 3
                exec("SELECT 1;")
                // NO OP
            }
            fail("Should have thrown ${GetConnectException::class.simpleName}")
        } catch (e: ExposedSQLException) {
            e.cause shouldBeInstanceOf GetConnectException::class
            datasource.connectCount shouldBeEqualTo 3
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
            // transaction block should use default DatabaseConfig values when no property is set
            transaction(Connection.TRANSACTION_SERIALIZABLE, db = db) {
                exec("SELECT 1;")
            }
            fail("Should have thrown ${GetConnectException::class.simpleName}")
        } catch (e: ExposedSQLException) {
            e.cause shouldBeInstanceOf GetConnectException::class
            datasource.connectCount shouldBeEqualTo 3
        }

        datasource.connectCount = 0

        try {
            // property set in transaction block should override default DatabaseConfig
            transaction(Connection.TRANSACTION_SERIALIZABLE, db = db) {
                maxAttempts = 5
                exec("SELECT 1;")
            }
            fail("Should have thrown ${GetConnectException::class.simpleName}")
        } catch (e: ExposedSQLException) {
            e.cause shouldBeInstanceOf GetConnectException::class
            datasource.connectCount shouldBeEqualTo 5
        }
    }
}
