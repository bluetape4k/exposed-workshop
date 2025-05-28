package exposed.examples.transactions

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.SQLException

class Ex06_RollbackTransaction: JdbcExposedTestBase() {

    companion object: KLogging()

    private fun countByValue(value: String): Int =
        RollbackTable.selectAll().where { RollbackTable.value eq value }.count().toInt()

    private fun allCount(): Int = RollbackTable.selectAll().count().toInt()

    /**
     * `save point` 없이 rollback 하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `rollback without save points`(testDB: TestDB) {
        withTables(testDB, RollbackTable) {
            inTopLevelTransaction(db.transactionManager.defaultIsolationLevel) {
                maxAttempts = 1
                RollbackTable.insert { it[value] = "before-dummy" }

                transaction {
                    countByValue("before-dummy") shouldBeEqualTo 1
                    RollbackTable.insert { it[value] = "inner-dummy" }
                }

                countByValue("before-dummy") shouldBeEqualTo 1
                countByValue("inner-dummy") shouldBeEqualTo 1

                RollbackTable.insert { it[value] = "after-dummy" }
                countByValue("after-dummy") shouldBeEqualTo 1

                // rollback() 를 호출하면, 모든 데이터의 추가가 취소됩니다.
                rollback()
            }
            // 위의 transaction이 rollback 되었으므로, 모든 데이터의 추가가 취소됩니다.
            countByValue("before-dummy") shouldBeEqualTo 0
            countByValue("inner-dummy") shouldBeEqualTo 0
            countByValue("after-dummy") shouldBeEqualTo 0
        }
    }

    /**
     * `save point`를 사용 (`useNestedTransactions = true`) 하여 rollback 하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `rollback with save points`(testDB: TestDB) {
        withTables(testDB, RollbackTable, configure = { useNestedTransactions = true }) {
            inTopLevelTransaction(db.transactionManager.defaultIsolationLevel) {
                maxAttempts = 1
                RollbackTable.insert { it[value] = "before-dummy" }

                // nested transaction이 rollback 되었으므로, `inner-dummy` 데이터의 추가가 취소됩니다.
                transaction {
                    countByValue("before-dummy") shouldBeEqualTo 1
                    RollbackTable.insert { it[value] = "inner-dummy" }
                    rollback()
                }

                countByValue("before-dummy") shouldBeEqualTo 1
                countByValue("inner-dummy") shouldBeEqualTo 0

                RollbackTable.insert { it[value] = "after-dummy" }
                countByValue("after-dummy") shouldBeEqualTo 1

                rollback()
            }
            // 위의 transaction이 rollback 되었으므로, 모든 데이터의 추가가 취소됩니다.
            countByValue("before-dummy") shouldBeEqualTo 0
            countByValue("inner-dummy") shouldBeEqualTo 0
            countByValue("after-dummy") shouldBeEqualTo 0
        }
    }

    /**
     * 예외로 인해 triggered 된 rollback 수행
     */
    @Test
    fun `rollback without save points triggered by exceptions`() {
        Assumptions.assumeTrue { TestDB.H2 in TestDB.enabledDialects() }

        TestDB.H2.connect()

        transaction {
            SchemaUtils.create(RollbackTable)
        }

        // database exception triggers rollback from inner to outer tx
        transaction {
            val fakeSQLString = "BROKEN_SQL_THAT_CAUSES_EXCEPTION"
            val outerTxId = this.id

            RollbackTable.insert { it[value] = "City A" }
            allCount() shouldBeEqualTo 1

            try {
                transaction {
                    val innerTxId = this.id
                    innerTxId shouldBeEqualTo outerTxId

                    RollbackTable.insert { it[value] = "City B" }
                    exec("${fakeSQLString}()")// 여기서 DB 예외가 발생하므로, 최종적으로 rollback이 발생해야 함
                }
                fail("Should have thrown an exception")
            } catch (cause: SQLException) {
                cause.toString() shouldContain fakeSQLString
            }

            // SQL 예외가 발생했으므로, rollback 되어 모든 데이터의 추가가 취소됩니다.
            allCount() shouldBeEqualTo 0
        }

        // db 예외가 아닌 경우 내부 Tx 에서 rollback이 발생하지 않으며, 외부 Tx에서 rollback이 발생하지 않도록 처리해야 함
        // 만약 이런 예외를 처리하지 않으면, 외부 Tx에서 rollback이 발생하게 되어 데이터가 삭제된다.
        transaction {
            val outerTxId = this.id

            RollbackTable.insert { it[value] = "City A" }
            allCount() shouldBeEqualTo 1

            try {
                transaction(db) {
                    val innerTxId = this.id
                    innerTxId shouldBeEqualTo outerTxId

                    RollbackTable.insert { it[value] = "City B" }
                    error("Failure")         // Application 예외가 발생하므로, rollback이 발생하지 않아야 함
                }
            } catch (cause: IllegalStateException) {
                cause.toString() shouldContain "Failure"
            }

            // Application 예외가 발생했으므로, rollback 되지 않아야 함 (rollback이 발생하지 않았으므로, 데이터가 그대로 남아 있어야 함)
            allCount() shouldBeEqualTo 2
        }

        transaction {
            SchemaUtils.drop(RollbackTable)
        }
    }
}
