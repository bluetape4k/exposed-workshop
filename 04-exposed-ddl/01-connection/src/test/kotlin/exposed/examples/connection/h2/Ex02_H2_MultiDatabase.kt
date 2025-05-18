package exposed.examples.connection.h2

import exposed.shared.dml.DMLTestData
import exposed.shared.samples.CountryTable
import exposed.shared.tests.TestDB
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.ifTrue
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.withSuspendTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.Executors

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Ex02_H2_MultiDatabase {

    companion object: KLoggingChannel()

    private val db1 by lazy {
        Database.connect(
            url = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
            user = "root",
            password = "",
            databaseConfig = DatabaseConfig {
                defaultIsolationLevel = java.sql.Connection.TRANSACTION_READ_COMMITTED
            }
        )
    }
    private val db2 by lazy {
        Database.connect(
            url = "jdbc:h2:mem:db2;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
            user = "root",
            password = "",
            databaseConfig = DatabaseConfig {
                defaultIsolationLevel = java.sql.Connection.TRANSACTION_READ_COMMITTED
            }
        )
    }

    private var currentDB: Database? = null

    @BeforeEach
    fun beforeEach() {
        Assumptions.assumeTrue { TestDB.H2 in TestDB.enabledDialects() }

        if (TransactionManager.isInitialized()) {
            currentDB = TransactionManager.currentOrNull()?.db
        }
    }

    @AfterEach
    fun afterEach() {
        // Assumptions.assumeTrue { TestDB.H2 in TestDB.enabledDialects() }
        TransactionManager.resetCurrent(currentDB?.transactionManager)
    }

    @Test
    fun `transaction with database`() {
        transaction(db1) {
            CountryTable.exists().shouldBeFalse()
            SchemaUtils.create(CountryTable)
            CountryTable.exists().shouldBeTrue()
            SchemaUtils.drop(CountryTable)
        }

        transaction(db2) {
            CountryTable.exists().shouldBeFalse()
            SchemaUtils.create(CountryTable)
            CountryTable.exists().shouldBeTrue()
            SchemaUtils.drop(CountryTable)
        }
    }

    @Test
    fun `simple insert in different databases`() {
        transaction(db1) {
            SchemaUtils.create(CountryTable)
            CountryTable.selectAll().empty().shouldBeTrue()
            CountryTable.insert {
                it[CountryTable.name] = "country1"
            }
        }

        transaction(db2) {
            SchemaUtils.create(CountryTable)
            CountryTable.selectAll().empty().shouldBeTrue()
            CountryTable.insert {
                it[CountryTable.name] = "country1"
            }
        }

        transaction(db1) {
            CountryTable.selectAll().count() shouldBeEqualTo 1L
            CountryTable.selectAll().single()[CountryTable.name] shouldBeEqualTo "country1"
            SchemaUtils.drop(CountryTable)
        }

        transaction(db2) {
            CountryTable.selectAll().count() shouldBeEqualTo 1L
            CountryTable.selectAll().single()[CountryTable.name] shouldBeEqualTo "country1"
            SchemaUtils.drop(CountryTable)
        }
    }


    @Test
    fun `Embedded Inserts In Different Database`() {
        transaction(db1) {
            SchemaUtils.create(DMLTestData.Cities)
            DMLTestData.Cities.selectAll().shouldBeEmpty()
            DMLTestData.Cities.insert {
                it[DMLTestData.Cities.name] = "city1"
            }

            transaction(db2) {
                DMLTestData.Cities.exists().shouldBeFalse()
                SchemaUtils.create(DMLTestData.Cities)
                DMLTestData.Cities.insert {
                    it[DMLTestData.Cities.name] = "city2"
                }
                DMLTestData.Cities.insert {
                    it[DMLTestData.Cities.name] = "city3"
                }
                DMLTestData.Cities.selectAll().count().toInt() shouldBeEqualTo 2
                DMLTestData.Cities.selectAll().last()[DMLTestData.Cities.name] shouldBeEqualTo "city3"
                SchemaUtils.drop(DMLTestData.Cities)
            }

            DMLTestData.Cities.selectAll().count() shouldBeEqualTo 1L
            DMLTestData.Cities.selectAll().single()[DMLTestData.Cities.name] shouldBeEqualTo "city1"
            SchemaUtils.drop(DMLTestData.Cities)
        }
    }

    @Test
    fun `Embedded Inserts In Different Database Depth2`() {
        transaction(db1) {
            SchemaUtils.create(DMLTestData.Cities)
            DMLTestData.Cities.selectAll().empty().shouldBeTrue()
            DMLTestData.Cities.insert {
                it[DMLTestData.Cities.name] = "city1"
            }

            transaction(db2) {
                DMLTestData.Cities.exists().shouldBeFalse()
                SchemaUtils.create(DMLTestData.Cities)
                DMLTestData.Cities.insert {
                    it[DMLTestData.Cities.name] = "city2"
                }
                DMLTestData.Cities.insert {
                    it[DMLTestData.Cities.name] = "city3"
                }
                DMLTestData.Cities.selectAll().count().toInt() shouldBeEqualTo 2
                DMLTestData.Cities.selectAll().last()[DMLTestData.Cities.name] shouldBeEqualTo "city3"

                transaction(db1) {
                    DMLTestData.Cities.selectAll().count() shouldBeEqualTo 1L
                    DMLTestData.Cities.insert {
                        it[DMLTestData.Cities.name] = "city4"
                    }
                    DMLTestData.Cities.insert {
                        it[DMLTestData.Cities.name] = "city5"
                    }
                    DMLTestData.Cities.selectAll().count() shouldBeEqualTo 3L
                }

                DMLTestData.Cities.selectAll().count() shouldBeEqualTo 2L
                DMLTestData.Cities.selectAll().last()[DMLTestData.Cities.name] shouldBeEqualTo "city3"
                SchemaUtils.drop(DMLTestData.Cities)
            }

            DMLTestData.Cities.selectAll().count() shouldBeEqualTo 3L

            DMLTestData.Cities.selectAll()
                .map {
                    it[DMLTestData.Cities.name]
                } shouldBeEqualTo listOf("city1", "city4", "city5")

            SchemaUtils.drop(DMLTestData.Cities)
        }
    }

    @Test
    fun `Coroutines With Multi Db`() = runSuspendIO {
        newSuspendedTransaction(Dispatchers.IO, db1) {
            val tr1 = this
            SchemaUtils.create(DMLTestData.Cities)
            DMLTestData.Cities.selectAll().empty().shouldBeTrue()
            DMLTestData.Cities.insert {
                it[DMLTestData.Cities.name] = "city1"
            }

            newSuspendedTransaction(Dispatchers.IO, db2) {
                DMLTestData.Cities.exists().shouldBeFalse()
                SchemaUtils.create(DMLTestData.Cities)
                DMLTestData.Cities.insert {
                    it[DMLTestData.Cities.name] = "city2"
                }
                DMLTestData.Cities.insert {
                    it[DMLTestData.Cities.name] = "city3"
                }
                DMLTestData.Cities.selectAll().count().toInt() shouldBeEqualTo 2
                DMLTestData.Cities.selectAll().last()[DMLTestData.Cities.name] shouldBeEqualTo "city3"

                tr1.withSuspendTransaction {
                    DMLTestData.Cities.selectAll().count() shouldBeEqualTo 1L
                    DMLTestData.Cities.insert {
                        it[DMLTestData.Cities.name] = "city4"
                    }
                    DMLTestData.Cities.insert {
                        it[DMLTestData.Cities.name] = "city5"
                    }
                    DMLTestData.Cities.selectAll().count() shouldBeEqualTo 3L
                }

                DMLTestData.Cities.selectAll().count() shouldBeEqualTo 2L
                DMLTestData.Cities.selectAll().last()[DMLTestData.Cities.name] shouldBeEqualTo "city3"
                SchemaUtils.drop(DMLTestData.Cities)
            }

            DMLTestData.Cities.selectAll().count().toInt() shouldBeEqualTo 3
            DMLTestData.Cities.selectAll()
                .map {
                    it[DMLTestData.Cities.name]
                } shouldBeEqualTo listOf("city1", "city4", "city5")

            SchemaUtils.drop(DMLTestData.Cities)
        }
    }

    @Test
    fun `when default database is not explicitly set - should return the latest connection`() {
        db1
        db2
        db2 shouldBeEqualTo TransactionManager.defaultDatabase
    }

    @Test
    fun `when default database is explicitly set - should return the set connection`() {
        db1
        db2
        TransactionManager.defaultDatabase = db1
        db1 shouldBeEqualTo TransactionManager.defaultDatabase
        TransactionManager.defaultDatabase = null
    }

    @Test
    fun `when set default database is removed - should return the latest connection`() {
        db1
        db2
        TransactionManager.defaultDatabase = db1
        TransactionManager.closeAndUnregister(db1)
        db2 shouldBeEqualTo TransactionManager.defaultDatabase
        TransactionManager.defaultDatabase = null
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @Test // this test always fails for one reason or another
    fun `when the default database is changed, coroutines should respect that`(): Unit = runSuspendIO {
        db1.name shouldBeEqualTo "jdbc:h2:mem:db1" // These two asserts fail sometimes for reasons that escape me
        db2.name shouldBeEqualTo "jdbc:h2:mem:db2" // but if you run just these tests one at a time, they pass.

        val coroutineDispatcher1 = newSingleThreadContext("first")
        TransactionManager.defaultDatabase = db1
        newSuspendedTransaction(coroutineDispatcher1) {
            TransactionManager.current().db.name shouldBeEqualTo db1.name
            // when running all tests together, this one usually fails
            // `Dual.select(intLiteral(1))`
            TransactionManager.current().exec("SELECT 1") { rs ->
                rs.next().ifTrue { rs.getInt(1) } shouldBeEqualTo 1
            }
        }
        TransactionManager.defaultDatabase = db2
        newSuspendedTransaction(coroutineDispatcher1) {
            TransactionManager.current().db.name shouldBeEqualTo db2.name // fails??
            TransactionManager.current().exec("SELECT 1") { rs ->
                rs.next().ifTrue { rs.getInt(1) } shouldBeEqualTo 1
            }
        }
        TransactionManager.defaultDatabase = null
    }

    @Test // If the first two assertions pass, the entire test passes
    fun `when the default database is changed, threads should respect that`() {
        db1.name shouldBeEqualTo "jdbc:h2:mem:db1"
        db2.name shouldBeEqualTo "jdbc:h2:mem:db2"
        val threadpool = Executors.newSingleThreadExecutor()
        TransactionManager.defaultDatabase = db1
        threadpool.submit {
            transaction {
                TransactionManager.current().db.name shouldBeEqualTo db1.name
                TransactionManager.current().exec("SELECT 1") { rs ->
                    rs.next().ifTrue { rs.getInt(1) } shouldBeEqualTo 1
                }
            }
        }.get()

        TransactionManager.defaultDatabase = db2
        threadpool.submit {
            transaction {
                TransactionManager.current().db.name shouldBeEqualTo db2.name
                TransactionManager.current().exec("SELECT 1") { rs ->
                    rs.next().ifTrue { rs.getInt(1) } shouldBeEqualTo 1
                }
            }
        }.get()
        TransactionManager.defaultDatabase = null
    }
}
