package exposed.examples.dml

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.jdbc.sql.getIntOrNull
import io.bluetape4k.jdbc.sql.map
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.ResultSet

class Ex50_RecursiveCTE: JdbcExposedTestBase() {

    companion object: KLogging()

    private val recursiveCTESupportedDb = TestDB.ALL_POSTGRES + TestDB.MYSQL_V8 + TestDB.MARIADB

    object Categories: IntIdTable("categories") {
        val parentId = integer("parent_id").nullable()
        val name = varchar("name", 50)
    }

    data class CategoryRecord(
        val id: Int,
        val parentId: Int?,
        val name: String,
        val path: String,
    )

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exec recursive cte`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in recursiveCTESupportedDb }

        withTables(testDB, Categories) {
            val rootId = Categories.insertAndGetId {
                it[name] = "Root"
                it[parentId] = null
            }

            val child1Id1 = Categories.insertAndGetId {
                it[name] = "Child 1"
                it[parentId] = rootId.value
            }
            val child1Id2 = Categories.insertAndGetId {
                it[name] = "Child 2"
                it[parentId] = rootId.value
            }

            Categories.insert {
                it[name] = "Grandchild 1.1"
                it[parentId] = child1Id1.value
            }

            Categories.insert {
                it[name] = "Grandchild 1.2"
                it[parentId] = child1Id1.value
            }

            val sql = when (testDB) {
                in TestDB.ALL_POSTGRES -> categoriesWithRecursiveForPostgres()
                in setOf(TestDB.MYSQL_V8, TestDB.MARIADB) -> categoriesWithRecursiveForMySQL()
                else -> throw IllegalStateException("Unsupported dialect for recursive CTE test: $testDB")
            }

            // val categoryRecords = mutableListOf<CategoryRecord>()
            val categoryRecords = exec(sql, explicitStatementType = StatementType.SELECT) { rs: ResultSet ->
                rs.map {
                    CategoryRecord(
                        id = rs.getInt("id"),
                        parentId = rs.getIntOrNull("parent_id"),
                        name = rs.getString("name"),
                        path = rs.getString("path")
                    ).apply {
                        log.debug { "Found category: name:${name}, path=${path}" }
                    }
                }
            }

            categoryRecords.shouldNotBeNull() shouldHaveSize 5
            categoryRecords[0].name shouldBeEqualTo "Root"
            categoryRecords[1].name shouldBeEqualTo "Child 1"
            categoryRecords[2].name shouldBeEqualTo "Child 2"
            categoryRecords[3].name shouldBeEqualTo "Grandchild 1.1"
            categoryRecords[4].name shouldBeEqualTo "Grandchild 1.2"
        }
    }

    private fun categoriesWithRecursiveForPostgres(): String = """
        WITH RECURSIVE recursive_categories AS (
            SELECT
                id,
                parent_id,
                name,
                id::text AS path
            FROM categories
            WHERE parent_id IS NULL
        
            UNION ALL
        
            SELECT
                c.id,
                c.parent_id,
                c.name,
                rc.path || '.' || c.id
            FROM categories c
            JOIN recursive_categories rc ON c.parent_id = rc.id
            WHERE POSITION('.' || c.id::text IN rc.path) = 0
        )
        SELECT *
        FROM recursive_categories;
    """.trimIndent()

    private fun categoriesWithRecursiveForMySQL(): String = """
        WITH RECURSIVE recursive_categories AS (
            SELECT
                id,
                parent_id,
                name,
                CAST(id AS CHAR(255)) AS path
            FROM categories
            WHERE parent_id IS NULL
        
            UNION ALL
        
            SELECT
                c.id,
                c.parent_id,
                c.name,
                CONCAT(rc.path, '.', c.id)
            FROM categories c
            JOIN recursive_categories rc ON c.parent_id = rc.id
            WHERE LOCATE(CONCAT('.', c.id), rc.path) = 0
        )
        SELECT *
        FROM recursive_categories;
    """.trimIndent()
}
