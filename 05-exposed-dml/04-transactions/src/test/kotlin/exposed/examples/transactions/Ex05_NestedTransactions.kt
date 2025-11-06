package exposed.examples.transactions

import exposed.shared.dml.DMLTestData
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.SQLException
import kotlin.test.fail

class Ex05_NestedTransactions: JdbcExposedTestBase() {

    companion object: KLogging()

    private val db by lazy {
        Database.connect(
            url = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
            user = "root",
            password = "",
            databaseConfig = DatabaseConfig {
                useNestedTransactions = true
                defaultMaxAttempts = 1
            }
        )
    }

    val cities = DMLTestData.Cities

    private fun cityCounts(): Int = cities.selectAll().count().toInt()

    private fun cityNames(): List<String> = cities.selectAll().map { it[cities.name] }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `중첩 트랜잭션 실행`(testDB: TestDB) {
        // 외부 트랜잭션
        withTables(testDB, cities, configure = { useNestedTransactions = true }) {
            cities.selectAll().shouldBeEmpty()
            cities.insert { it[name] = "city1" }
            cityCounts() shouldBeEqualTo 1
            cityNames() shouldBeEqualTo listOf("city1")

            // 중첩 1
            transaction {
                cities.insert {
                    it[name] = "city2"
                }
                cityNames() shouldBeEqualTo listOf("city1", "city2")

                // 중첩 2
                transaction {
                    cities.insert { it[name] = "city3" }
                    cityNames() shouldBeEqualTo listOf("city1", "city2", "city3")
                }
                // 중첩 2가 성공했으므로, 중접 1의 결과는 모두 반영되어야 한다.
                cityNames() shouldBeEqualTo listOf("city1", "city2", "city3")

                // 중첩1을 강제 롤백한다
                rollback()
            }

            // 중첩1과 내부 트랜잭션의 작업은 모두 취소되고, 현재 트랜잭션 결과만 반영된다.
            cityNames() shouldBeEqualTo listOf("city1")
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `outer transaction restored after nested transaction failed`(testDB: TestDB) {
        withTables(testDB, cities) {
            TransactionManager.currentOrNull().shouldNotBeNull()

            try {
                inTopLevelTransaction(transactionIsolation = this.transactionIsolation) {
                    maxAttempts = 1
                    throw IllegalStateException("Should be rethrow")
                }
            } catch (e: Exception) {
                e shouldBeInstanceOf IllegalStateException::class
            }

            TransactionManager.currentOrNull().shouldNotBeNull()
        }
    }

    @Test
    fun `nested transaction not committed after database failure`() {
        Assumptions.assumeTrue { TestDB.H2 in TestDB.enabledDialects() }

        val fakeSQLString = "BROKEN_SQL_THAT_CAUSES_EXCEPTION"

        transaction(db) {
            SchemaUtils.create(cities)
        }

        transaction(db) {
            val outerTxId = this.id

            cities.insert { it[name] = "City A" }
            cityCounts() shouldBeEqualTo 1

            try {
                inTopLevelTransaction(db = db, transactionIsolation = db.transactionManager.defaultIsolationLevel) {
                    val innerTxId = this.id
                    innerTxId shouldNotBeEqualTo outerTxId

                    cities.insert { it[name] = "City B" }           // 이 작업는 롤백됩니다.
                    exec("${fakeSQLString}();")
                }
                fail("Should not reach here")
            } catch (cause: SQLException) {
                cause.toString() shouldContain fakeSQLString
            }
        }

        assertSingleRecordInNewTransactionAndReset()

        transaction(db) {
            val outerTxId = this.id

            cities.insert { it[cities.name] = "City A" }
            cityCounts() shouldBeEqualTo 1

            try {
                transaction(db) {
                    val innerTxId = this.id
                    innerTxId shouldNotBeEqualTo outerTxId

                    cities.insert { it[cities.name] = "City B" }      // 이 작업는 롤백됩니다.
                    exec("SELECT * FROM non_existent_table")
                }
                fail("Should not reach here")
            } catch (cause: SQLException) {
                cause.toString() shouldContain "non_existent_table"
            }
        }

        assertSingleRecordInNewTransactionAndReset()

        transaction(db) {
            SchemaUtils.drop(cities)
        }
    }

    @Test
    fun `nested transaction not committed after exception`() {
        Assumptions.assumeTrue { TestDB.H2 in TestDB.enabledDialects() }

        val exceptionMessage = "Failure!"

        transaction(db) {
            SchemaUtils.create(cities)
        }

        transaction(db) {
            val outerTxId = this.id
            cities.insert { it[name] = "City A" }
            cities.selectAll().count().toInt() shouldBeEqualTo 1

            try {
                inTopLevelTransaction(db = db, transactionIsolation = db.transactionManager.defaultIsolationLevel) {
                    val innerTxId = this.id
                    innerTxId shouldNotBeEqualTo outerTxId

                    cities.insert { it[name] = "City B" }       // 이 코드는 실행되지 않는다.
                    error(exceptionMessage)
                }
            } catch (cause: IllegalStateException) {
                cause.toString() shouldContain exceptionMessage
            }
        }

        assertSingleRecordInNewTransactionAndReset()

        transaction(db) {
            val outerTxId = this.id
            cities.insert { it[name] = "City A" }
            cities.selectAll().count().toInt() shouldBeEqualTo 1

            try {
                transaction(db) {
                    val innerTxId = this.id
                    innerTxId shouldNotBeEqualTo outerTxId

                    cities.insert { it[name] = "City B" }       // 이 코드는 실행되지 않는다.
                    error(exceptionMessage)
                }
            } catch (cause: IllegalStateException) {
                cause.toString() shouldContain exceptionMessage
            }
        }
        assertSingleRecordInNewTransactionAndReset()

        transaction(db) {
            SchemaUtils.drop(cities)
        }

    }

    private fun assertSingleRecordInNewTransactionAndReset() = transaction(db) {
        val result = cities.selectAll().single()[cities.name]
        result shouldBeEqualTo "City A"
        cities.deleteAll()
    }
}
