package exposed.examples.dml

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.exposed.core.CteTable
import io.bluetape4k.exposed.jdbc.withCte
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.crossJoin
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * `CteTable` 과 `withCte` 로 Exposed Query Builder 기반 CTE를 작성하는 예제.
 *
 * Raw SQL 문자열 대신 Exposed DSL 쿼리를 CTE 본문으로 재사용하고, CTE 임시 테이블의 컬럼을
 * `cte[원본컬럼]` 형태로 참조합니다.
 */
class Ex51_CteQueryBuilder: AbstractExposedTest() {

    companion object: KLogging() {
        private val cteSupportedDb = TestDB.ALL_H2 + TestDB.ALL_POSTGRES_LIKE + TestDB.MYSQL_V8
    }

    object CteUsers: Table("cte_query_builder_users") {
        val id = integer("id")
        val name = varchar("name", 64)
        val active = bool("active")
        val managerId = integer("manager_id").nullable()

        override val primaryKey = PrimaryKey(id)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `query builder cte filters active users`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in cteSupportedDb }

        withTables(testDB, CteUsers) {
            seedUsers()

            val activeUsers = CteTable(
                name = "active_users",
                query = CteUsers
                    .select(CteUsers.id, CteUsers.name)
                    .where { CteUsers.active eq true }
            )
            val activeName = activeUsers[CteUsers.name]
            val query = activeUsers
                .select(activeName)
                .withCte(activeUsers)
                .orderBy(activeUsers[CteUsers.id])

            val builder = QueryBuilder(prepared = true)
            val sql = query.prepareSQL(builder)

            sql shouldContain "WITH"
            sql.lowercase() shouldContain "active_users"
            builder.args.map { it.second } shouldBeEqualTo listOf(true)

            query.map { it[activeName] } shouldBeEqualTo listOf("root", "child")
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `recursive query builder cte references temporary table columns`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in cteSupportedDb }

        withTables(testDB, CteUsers) {
            seedUsers()

            val hierarchy = CteTable(
                name = "user_hierarchy",
                query = CteUsers
                    .select(CteUsers.id, CteUsers.name, CteUsers.managerId)
                    .where { CteUsers.managerId.isNull() and (CteUsers.active eq true) },
                recursiveQuery = { cte ->
                    CteUsers
                        .crossJoin(cte)
                        .select(CteUsers.id, CteUsers.name, CteUsers.managerId)
                        .where { CteUsers.managerId eq cte[CteUsers.id] }
                }
            )
            val hierarchyName = hierarchy[CteUsers.name]
            val query = hierarchy
                .select(hierarchyName)
                .withCte(hierarchy)
                .orderBy(hierarchy[CteUsers.id])

            val sql = query.prepareSQL(QueryBuilder(prepared = true))

            sql shouldContain "WITH RECURSIVE"
            query.map { it[hierarchyName] } shouldBeEqualTo listOf("root", "child", "inactive-child")
        }
    }

    private fun seedUsers() {
        CteUsers.insert {
            it[id] = 1
            it[name] = "root"
            it[active] = true
            it[managerId] = null
        }
        CteUsers.insert {
            it[id] = 2
            it[name] = "child"
            it[active] = true
            it[managerId] = 1
        }
        CteUsers.insert {
            it[id] = 3
            it[name] = "inactive-child"
            it[active] = false
            it[managerId] = 2
        }
        CteUsers.insert {
            it[id] = 4
            it[name] = "other-root"
            it[active] = false
            it[managerId] = null
        }
    }
}
