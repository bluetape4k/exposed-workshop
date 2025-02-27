package exposed.examples.transactions

import exposed.shared.dml.DMLTestData
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.expectException
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEqualTo
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.inTopLevelTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.concurrent.thread
import kotlin.test.assertFails
import kotlin.test.fail

class Ex07_ThreadLocalManager: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * MYSQL V5 에서만 지원됩니다.
     */
    fun `re-connection`() {
        Assumptions.assumeTrue { TestDB.MYSQL_V5 in TestDB.enabledDialects() }

        var secondThreadTm: TransactionManager? = null
        val db1: Database = TestDB.MYSQL_V5.connect()
        lateinit var db2: Database

        transaction {
            val firstThreadTm = db1.transactionManager
            SchemaUtils.create(DMLTestData.Cities)

            thread {
                db2 = TestDB.MYSQL_V5.connect()
                transaction {
                    DMLTestData.Cities.selectAll().toList()
                    secondThreadTm = db2.transactionManager
                    secondThreadTm shouldNotBeEqualTo firstThreadTm
                }
            }.join()
            db1.transactionManager shouldBeEqualTo firstThreadTm
            SchemaUtils.drop(DMLTestData.Cities)
        }
        db2.transactionManager shouldBeEqualTo secondThreadTm
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testReadOnly(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in READ_ONLY_EXCLUDED_VENDORS }

        withTables(testDB, RollbackTable) {
            assertFails {
                // read-only 이므로 INSERT 작업은 실패합니다.
                inTopLevelTransaction(db.transactionManager.defaultIsolationLevel, true) {
                    maxAttempts = 1
                    RollbackTable.insert { it[value] = "random-something" }
                }
            }.message
                ?.let { it shouldContain "read-only" }
                ?: fail("error message should not be null")
        }
    }

    /**
     * Coroutines 환경에서 [newSuspendedTransaction] 으로 readOnly 작업을 수행하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `suspended read-only`(testDB: TestDB) = runTest {
        Assumptions.assumeTrue { testDB !in READ_ONLY_EXCLUDED_VENDORS }

        val database: Database = testDB.connect()

        // 읽기 전용에서 테이블 생성은 실패합니다.
        newSuspendedTransaction(db = database, readOnly = true) {
            expectException<ExposedSQLException> {
                SchemaUtils.create(RollbackTable)
            }
        }

        transaction(db = database) {
            SchemaUtils.create(RollbackTable)
        }

        // 읽기 전용에서 데이터 추가는 실패합니다.
        newSuspendedTransaction(db = database, readOnly = true) {
            expectException<ExposedSQLException> {
                RollbackTable.insert { it[value] = "random-something" }
            }
        }

        transaction(db = database) {
            RollbackTable.insert { it[value] = "random-something" }
        }

        transaction(db = database) {
            SchemaUtils.drop(RollbackTable)
        }
    }
}
