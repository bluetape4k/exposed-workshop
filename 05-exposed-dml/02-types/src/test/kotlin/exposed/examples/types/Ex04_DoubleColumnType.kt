package exposed.examples.types

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex04_DoubleColumnType: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS double_table (
     *      id SERIAL PRIMARY KEY,
     *      amount DOUBLE PRECISION NOT NULL
     * )
     * ```
     */
    object TestTable: IntIdTable("double_table") {
        val amount = double("amount")
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and select from double column`(testDB: TestDB) {
        withTables(testDB, TestTable) {
            val id = TestTable.insertAndGetId {
                it[amount] = 9.23
            }

            TestTable.selectAll()
                .where { TestTable.id eq id }
                .single()[TestTable.amount] shouldBeEqualTo 9.23
        }
    }

    /**
     * `DOUBLE PRECISION` 타입을 사용하는 컬럼을 `REAL` 타입으로 변경해도 작동한다
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and select from real column`(testDB: TestDB) {
        withDb(testDB) {
            val originalColumnDDL = TestTable.amount.descriptionDdl()
            val realColumnDDL = originalColumnDDL.replace(" DOUBLE PRECISION ", " REAL ")

            /**
             * create table with double() column that uses SQL type REAL
             *
             * ```sql
             * -- Postgres
             * CREATE TABLE IF NOT EXISTS double_table (
             *      id SERIAL PRIMARY KEY,
             *      amount REAL NOT NULL
             * )
             * ```
             */
            TestTable.ddl
                .map { it.replace(originalColumnDDL, realColumnDDL) }
                .forEach { exec(it) }

            val id = TestTable.insertAndGetId {
                it[amount] = 9.23
            }

            TestTable.selectAll()
                .where { TestTable.id eq id }
                .single()[TestTable.amount] shouldBeEqualTo 9.23

            SchemaUtils.drop(TestTable)
        }
    }
}
