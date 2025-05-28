package exposed.examples.transactions

import com.impossibl.postgres.jdbc.PGSQLSimpleException
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeInstanceOf
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.util.PSQLException
import java.sql.SQLException
import java.sql.SQLSyntaxErrorException
import java.sql.SQLTimeoutException
import kotlin.test.DefaultAsserter.fail

/**
 * 쿼리 실행 시 다음과 같이 Timeout 을 설정할 수 있습니다.
 *
 * - [org.jetbrains.exposed.sql.Transaction.queryTimeout] 을 설정하여 SQL 쿼리 실행 시 Timeout 을 설정
 * - [org.jetbrains.exposed.sql.Transaction.queryTimeout] = 0 이면 Timeout 이 없음을 의미
 * - [org.jetbrains.exposed.sql.Transaction.queryTimeout] < 0 이면 예외가 발생
 */
class Ex04_QueryTimeout: JdbcExposedTestBase() {

    companion object: KLogging()

    private fun generateTimeoutStatement(db: TestDB, timeout: Int): String {
        return when (db) {
            in TestDB.ALL_MYSQL_MARIADB -> "SELECT SLEEP($timeout) = 0;"
            in TestDB.ALL_POSTGRES -> "SELECT pg_sleep($timeout);"
            else -> throw NotImplementedError()
        }
    }

    // MySQL V5 는 제외되었습니다. "java.lang.NoClassDefFoundError: com/mysql/cj/jdbc/exceptions/MySQLTimeoutException" 오류가 발생합니다.
    // 아마도 org.jetbrains.exposed.sql.Database::driverMapping 에서 모든 Driver 버전이 동일한 패키지를 가져야 한다고 기대하는 것 같습니다.
    private val timeoutTestDBList = TestDB.ALL_POSTGRES + TestDB.ALL_MARIADB + TestDB.MYSQL_V8

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `timeout statements`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in timeoutTestDBList }

        withDb(testDB) {
            this.queryTimeout = 3  // 3초로 설정합니다.
            try {
                /**
                 * ```sql
                 * SELECT SLEEP(5) = 0;   -- MySQL
                 * SELECT pg_sleep(5);    -- Postgre
                 * ```
                 */
                val stmt = generateTimeoutStatement(it, 5).apply {
                    log.debug { "Executing statement: $this" }
                }
                TransactionManager.current().exec(stmt)

                fail("타임아웃 또는 취소된 statement 예외가 발생해야 합니다.")
            } catch (cause: ExposedSQLException) {
                when (testDB) {
                    // PostgreSQL 은 취소된 statement message를 포함한 표준 [PSQLException] 을 던집니다.
                    TestDB.POSTGRESQL -> cause.cause shouldBeInstanceOf PSQLException::class
                    // PostgreSQLNG 은 취소된 statement message를 포함한 표준 [PGSQLSimpleException] 을 던집니다.
                    TestDB.POSTGRESQLNG -> cause.cause shouldBeInstanceOf PGSQLSimpleException::class
                    else -> cause.cause shouldBeInstanceOf SQLTimeoutException::class
                }
            }
        }
    }

    /**
     * queryTimeout = 3 이므로, sleep(1) 은 실행됩니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `no timeout with timeout statement`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in timeoutTestDBList }

        withDb(testDB) {
            this.queryTimeout = 3
            TransactionManager.current()
                .exec(generateTimeoutStatement(it, 1))
        }
    }

    /**
     * queryTimeout = 0 는 timeout 이 없음을 의미합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `timeout zero with timeout statement`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in timeoutTestDBList }

        withDb(testDB) {
            // queryTimeout = 0 는 timeout 이 없음을 의미합니다.
            this.queryTimeout = 0
            TransactionManager.current()
                .exec(generateTimeoutStatement(it, 1))
        }
    }

    /**
     * queryTimeout = -1 은 예외가 발생합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `timeout minus with timeout statement`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in timeoutTestDBList }

        withDb(testDB) {
            this.queryTimeout = -1
            try {
                TransactionManager.current()
                    .exec(generateTimeoutStatement(it, 1))

                fail("타임아웃 또는 취소된 statement 예외가 발생해야 합니다.")
            } catch (cause: ExposedSQLException) {
                log.debug(cause) { "queryTimeout: -1" }

                when (testDB) {
                    // PostgreSQL 은 취소된 statement message를 포함한 표준 [PSQLException] 을 던집니다.
                    // Query timeout 은 0 이상의 값이어야 합니다.
                    TestDB.POSTGRESQL -> cause.cause shouldBeInstanceOf PSQLException::class

                    // MySQL, POSTGRESQLNG 는 -1 타임아웃 값으로 일반 [SQLException] 을 던집니다.
                    in (TestDB.ALL_MYSQL + TestDB.POSTGRESQLNG) -> cause.cause shouldBeInstanceOf SQLException::class

                    // MariaDB는 음의 timeout 값으로 일반 [SQLSyntaxErrorException] 을 던집니다.
                    in TestDB.ALL_MARIADB -> cause.cause shouldBeInstanceOf SQLSyntaxErrorException::class

                    // SqlServer throws a regular SQLServerException with a minus timeout value
                    // TestDB.SQLSERVER -> assertTrue(cause.cause is SQLServerException)
                    else -> throw NotImplementedError()
                }
            }
        }
    }
}
