package exposed.shared.tests

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.*

class TestUtilsTest: AbstractExposedTest() {

    companion object: KLogging()

    private object TestTable: IntIdTable("test_table") {
        val name = varchar("name", 255)
    }

    @Test
    fun `inProperCase should return proper case string`() {
        withTables(TestDB.H2, TestTable) {
            val columnName = TestTable.name.name
            columnName.shouldNotBeNull()
        }
    }

    @Test
    fun `inProperCase should return original string when no transaction`() {
        val result = "test_column".inProperCase()
        result shouldBeEqualTo "test_column"
    }

    @Test
    fun `currentDialectTest should return dialect from transaction`() {
        withDb(TestDB.H2) {
            val dialect = currentDialectTest
            dialect.shouldNotBeNull()
            dialect.name shouldBeEqualTo "H2"
        }
    }

    @Test
    fun `currentDialectIfAvailableTest should return dialect when in transaction`() {
        withDb(TestDB.H2) {
            val dialect = currentDialectIfAvailableTest
            dialect.shouldNotBeNull()
        }
    }

    @Test
    fun `currentDialectIfAvailableTest should return null when no transaction`() {
        val dialect = currentDialectIfAvailableTest
        dialect shouldBeEqualTo null
    }

    @Test
    fun `enumSetOf should create enum set with elements`() {
        enumSetOf(TestDB.H2, TestDB.H2_MYSQL).let { set ->
            set shouldBeEqualTo EnumSet.of(TestDB.H2, TestDB.H2_MYSQL)
        }
    }

    @Test
    fun `enumSetOf should create empty enum set when no elements`() {
        enumSetOf<TestDB>().let { set ->
            set.isEmpty() shouldBeEqualTo true
        }
    }

    @Test
    fun `insertAndWait should insert and wait specified duration`() {
        withTables(TestDB.H2, TestTable) {
            val startTime = System.currentTimeMillis()
            TestTable.insertAndWait(100) {
                it[TestTable.name] = Base58.randomString(8)
            }
            val elapsed = System.currentTimeMillis() - startTime

            (elapsed >= 100) shouldBeEqualTo true
        }
    }

    enum class TestEnum {
        FIRST,
        SECOND,
        THIRD,
    }
}
