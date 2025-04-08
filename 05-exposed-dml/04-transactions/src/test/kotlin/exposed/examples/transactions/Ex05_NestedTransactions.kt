package exposed.examples.transactions

import exposed.shared.dml.DMLTestData
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.inTopLevelTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.SQLException
import kotlin.test.fail

class Ex05_NestedTransactions: AbstractExposedTest() {

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

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `nested transactions`(testDB: TestDB) {
        val cities = DMLTestData.Cities
        withTables(testDB, cities, configure = { useNestedTransactions = true }) {
            cities.selectAll().shouldBeEmpty()

            cities.insert {
                it[name] = "city1"
            }

            cities.selectAll().count().toInt() shouldBeEqualTo 1
            cities.selectAll().map { it[cities.name] } shouldBeEqualTo listOf("city1")

            transaction {
                cities.insert {
                    it[name] = "city2"
                }
                cities.selectAll().map { it[cities.name] } shouldBeEqualTo listOf("city1", "city2")

                transaction {
                    cities.insert {
                        it[name] = "city3"
                    }
                    cities.selectAll().map { it[cities.name] } shouldBeEqualTo listOf("city1", "city2", "city3")
                }

                cities.selectAll().map { it[cities.name] } shouldBeEqualTo listOf("city1", "city2", "city3")

                rollback()
            }

            cities.selectAll().map { it[cities.name] } shouldBeEqualTo listOf("city1")
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `outer transaction restored after nested transaction failed`(testDB: TestDB) {
        val cities = DMLTestData.Cities
        withTables(testDB, cities) {
            TransactionManager.currentOrNull().shouldNotBeNull()

            try {
                inTopLevelTransaction(this.transactionIsolation) {
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

        val cities = DMLTestData.Cities
        val fakeSQLString = "BROKEN_SQL_THAT_CAUSES_EXCEPTION"

        transaction(db) {
            SchemaUtils.create(cities)
        }

        transaction(db) {
            val outerTxId = this.id

            cities.insert { it[name] = "City A" }
            cities.selectAll().count().toInt() shouldBeEqualTo 1

            try {
                inTopLevelTransaction(db.transactionManager.defaultIsolationLevel, db = db) {
                    val innerTxId = this.id
                    innerTxId shouldNotBeEqualTo outerTxId

                    cities.insert { it[name] = "City B" }           // 이 코드는 실행되지 않는다.
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
            cities.selectAll().count().toInt() shouldBeEqualTo 1

            try {
                transaction(db) {
                    val innerTxId = this.id
                    innerTxId shouldNotBeEqualTo outerTxId

                    cities.insert { it[cities.name] = "City B" }      // 이 코드는 실행되지 않는다.
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
        val cities = DMLTestData.Cities

        transaction(db) {
            SchemaUtils.create(cities)
        }

        transaction(db) {
            val outerTxId = this.id
            cities.insert { it[name] = "City A" }
            cities.selectAll().count().toInt() shouldBeEqualTo 1

            try {
                inTopLevelTransaction(db.transactionManager.defaultIsolationLevel, db = db) {
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
        val result = DMLTestData.Cities.selectAll().single()[DMLTestData.Cities.name]
        result shouldBeEqualTo "City A"
        DMLTestData.Cities.deleteAll()
    }
}
