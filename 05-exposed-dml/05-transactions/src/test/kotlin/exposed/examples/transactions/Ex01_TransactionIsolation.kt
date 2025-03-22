package exposed.examples.transactions

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.inTopLevelTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Connection

/**
 * Connection의 Transaction Isolation Level을 설정하는 방법에 대한 테스트 코드입니다.
 */
class Ex01_TransactionIsolation: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * `@@tx_isolation` is deprecated in MySQL 8.0.3 and removed in 8.0.4
     */
    private val transactionIsolationSupportDB =
        TestDB.ALL_MARIADB + TestDB.MYSQL_V5 + TestDB.POSTGRESQL

    private val isolations = listOf(
        // Connection.TRANSACTION_NONE,              // not supported
        Connection.TRANSACTION_READ_UNCOMMITTED,
        Connection.TRANSACTION_READ_COMMITTED,
        Connection.TRANSACTION_REPEATABLE_READ,
        Connection.TRANSACTION_SERIALIZABLE,
    )

    @Disabled("TestDB는 모두 HikariCP 를 사용하도록 변경해서 Exposed의 Databasse의 transactionIsolation 이 적용되지 않음")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `what transaction isolation was applied`(testDB: TestDB) {

        Assumptions.assumeTrue { testDB in transactionIsolationSupportDB }

        isolations.forEach { isolation ->
            withDb(testDB) {
                log.debug { "db: ${testDB.name}, isolation: $isolation" }
                inTopLevelTransaction(isolation) {
                    maxAttempts = 1
                    this.connection.transactionIsolation shouldBeEqualTo isolation
                }
            }
        }
    }

    /**
     * HikariCP 에서 Transaction Isolation 을 설정하는 방법
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `transaction isolation with HikariDataSource`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in transactionIsolationSupportDB }

        val db = Database.connect(
            HikariDataSource(setupHikariConfig(testDB, "TRANSACTION_REPEATABLE_READ"))
        )
        val manager: TransactionManager = TransactionManager.managerFor(db)!!

        transaction(db) {
            // transaction manager should use database default since no level is provided other than hikari
            manager.defaultIsolationLevel shouldBeEqualTo Database.getDefaultIsolationLevel(db)

            // database level should be set by hikari dataSource
            assertTransactionIsolationLevel(testDB, Connection.TRANSACTION_REPEATABLE_READ)

            // after first connection, transaction manager should use hikari level by default
            manager.defaultIsolationLevel shouldBeEqualTo Connection.TRANSACTION_REPEATABLE_READ
        }

        transaction(transactionIsolation = Connection.TRANSACTION_READ_COMMITTED, db = db) {
            manager.defaultIsolationLevel shouldBeEqualTo Connection.TRANSACTION_REPEATABLE_READ

            // database level should be set by transaction-specific setting
            assertTransactionIsolationLevel(testDB, Connection.TRANSACTION_READ_COMMITTED)
        }

        transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, db = db) {
            manager.defaultIsolationLevel shouldBeEqualTo Connection.TRANSACTION_REPEATABLE_READ

            // database level should be set by transaction-specific setting
            assertTransactionIsolationLevel(testDB, Connection.TRANSACTION_SERIALIZABLE)
        }

        transaction(db) {
            manager.defaultIsolationLevel shouldBeEqualTo Connection.TRANSACTION_REPEATABLE_READ

            // database level should be set by hikari dataSource
            assertTransactionIsolationLevel(testDB, Connection.TRANSACTION_REPEATABLE_READ)
        }

        TransactionManager.closeAndUnregister(db)
    }

    /**
     * HikariCP 는 `TRANSACTION_REPEATABLE_READ` 로 설정,
     * Exposed Connection 은 `TRANSACTION_READ_COMMITTED` 로 설정했을 때
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `transaction isolation with Hikari and Database Config`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in transactionIsolationSupportDB }

        val db = Database.connect(
            HikariDataSource(setupHikariConfig(testDB, "TRANSACTION_REPEATABLE_READ")),
            databaseConfig = DatabaseConfig { defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED }
        )

        val manager = TransactionManager.managerFor(db)!!

        transaction(db) {
            // transaction manager should default to use DatabaseConfig level
            manager.defaultIsolationLevel shouldBeEqualTo Connection.TRANSACTION_READ_COMMITTED

            // database level should be set by DatabaseConfig
            assertTransactionIsolationLevel(testDB, Connection.TRANSACTION_READ_COMMITTED)
            // after first connection, transaction manager should retain DatabaseConfig level
            manager.defaultIsolationLevel shouldBeEqualTo Connection.TRANSACTION_READ_COMMITTED
        }

        transaction(transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ, db = db) {
            manager.defaultIsolationLevel shouldBeEqualTo Connection.TRANSACTION_READ_COMMITTED

            // database level should be set by transaction-specific setting
            assertTransactionIsolationLevel(testDB, Connection.TRANSACTION_REPEATABLE_READ)
        }

        transaction(transactionIsolation = Connection.TRANSACTION_SERIALIZABLE, db = db) {
            manager.defaultIsolationLevel shouldBeEqualTo Connection.TRANSACTION_READ_COMMITTED

            // database level should be set by transaction-specific setting
            assertTransactionIsolationLevel(testDB, Connection.TRANSACTION_SERIALIZABLE)
        }

        transaction(db) {
            manager.defaultIsolationLevel shouldBeEqualTo Connection.TRANSACTION_READ_COMMITTED

            // database level should be set by DatabaseConfig
            assertTransactionIsolationLevel(testDB, Connection.TRANSACTION_READ_COMMITTED)
        }

        TransactionManager.closeAndUnregister(db)
    }

    private fun setupHikariConfig(testDB: TestDB, isolation: String): HikariConfig {
        return HikariConfig().apply {
            jdbcUrl = testDB.connection.invoke()
            driverClassName = testDB.driver
            username = testDB.user
            password = testDB.pass
            maximumPoolSize = 6
            isAutoCommit = false
            transactionIsolation = isolation
            validate()
        }
    }

    private fun Transaction.assertTransactionIsolationLevel(testDB: TestDB, expected: Int) {
        val (sql, repeatable, committed, serializable) = when (testDB) {
            TestDB.POSTGRESQL -> listOf(
                "SHOW TRANSACTION ISOLATION LEVEL",
                "repeatable read",
                "read committed",
                "serializable"
            )
            in TestDB.ALL_MYSQL_MARIADB -> listOf(
                "SELECT @@tx_isolation",
                "REPEATABLE-READ",
                "READ-COMMITTED",
                "SERIALIZABLE"
            )
            else -> throw UnsupportedOperationException("Unsupported testDB: $testDB")
        }

        val expectedLevel = when (expected) {
            Connection.TRANSACTION_READ_COMMITTED -> committed
            Connection.TRANSACTION_REPEATABLE_READ -> repeatable
            Connection.TRANSACTION_SERIALIZABLE -> serializable
            else -> throw UnsupportedOperationException("Unsupported transaction isolation level: $expected")
        }

        val actual = exec("$sql;") { resultSet ->
            resultSet.next()
            resultSet.getString(1)
        }
        log.info { "sql=$sql, actual=$actual, expected=$expectedLevel" }
        actual.shouldNotBeNull() shouldBeEqualTo expectedLevel
    }
}
