package exposed.examples.entities

import exposed.shared.dml.DMLTestData
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * 테이블 간의 참조 관계에 따라 Schema 생성과 관련된 검증
 */
class Ex31_SelfReference: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun simpleTest(testDB: TestDB) {
        SchemaUtils.sortTablesByReferences(listOf(DMLTestData.Cities)) shouldBeEqualTo listOf(DMLTestData.Cities)
        SchemaUtils.sortTablesByReferences(listOf(DMLTestData.Users)) shouldBeEqualTo listOf(
            DMLTestData.Cities,
            DMLTestData.Users
        )

        val rightOrder = listOf(DMLTestData.Cities, DMLTestData.Users, DMLTestData.UserData)
        val r1 = SchemaUtils.sortTablesByReferences(listOf(DMLTestData.Cities, DMLTestData.UserData, DMLTestData.Users))
        val r2 = SchemaUtils.sortTablesByReferences(listOf(DMLTestData.UserData, DMLTestData.Cities, DMLTestData.Users))
        val r3 = SchemaUtils.sortTablesByReferences(listOf(DMLTestData.Users, DMLTestData.Cities, DMLTestData.UserData))

        r1 shouldBeEqualTo rightOrder
        r2 shouldBeEqualTo rightOrder
        r3 shouldBeEqualTo rightOrder
    }

    object TestTables {
        object Cities: Table() {
            val id = integer("id").autoIncrement()
            val name = varchar("name", 50)
            val strange_id = varchar("strange_id", 10).references(StrangeTable.id)

            override val primaryKey = PrimaryKey(id)
        }

        object Users: Table() {
            val id = varchar("id", 10)
            val name = varchar("name", 50)
            val city_id = integer("city_id").references(Cities.id).nullable()

            override val primaryKey = PrimaryKey(id)
        }

        object NoRefereeTable: Table() {
            val id = varchar("id", 10)
            val col1 = varchar("col1", 10)

            override val primaryKey = PrimaryKey(id)
        }

        object RefereeTable: Table() {
            val id = varchar("id", 10)
            val ref = reference("ref", NoRefereeTable.id)

            override val primaryKey = PrimaryKey(id)
        }

        object ReferencedTable: IntIdTable() {
            val col3 = varchar("col3", 10)
        }

        object StrangeTable: Table() {
            val id = varchar("id", 10)
            val user_id = varchar("user_id", 10).references(Users.id)
            val comment = varchar("comment", 30)
            val value = integer("value")

            override val primaryKey = PrimaryKey(id)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `cycle references check`(testDB: TestDB) {
        val original = listOf(
            TestTables.Cities,
            TestTables.Users,
            TestTables.StrangeTable,
            TestTables.NoRefereeTable,
            TestTables.RefereeTable,
            TestTables.ReferencedTable,
        )
        val sortedTables = SchemaUtils.sortTablesByReferences(original)
        val expected = listOf(
            TestTables.Users,
            TestTables.StrangeTable,
            TestTables.Cities,
            TestTables.NoRefereeTable,
            TestTables.RefereeTable,
            TestTables.ReferencedTable,
        )
        sortedTables shouldBeEqualTo expected
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `has cycle`(testDB: TestDB) {
        SchemaUtils.checkCycle(TestTables.ReferencedTable).shouldBeFalse()
        SchemaUtils.checkCycle(TestTables.RefereeTable).shouldBeFalse()
        SchemaUtils.checkCycle(TestTables.NoRefereeTable).shouldBeFalse()
        SchemaUtils.checkCycle(TestTables.Users).shouldBeTrue()
        SchemaUtils.checkCycle(TestTables.Cities).shouldBeTrue()
        SchemaUtils.checkCycle(TestTables.StrangeTable).shouldBeTrue()
    }
}
