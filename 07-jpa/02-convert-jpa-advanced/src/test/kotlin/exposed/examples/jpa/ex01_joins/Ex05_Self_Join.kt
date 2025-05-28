package exposed.examples.jpa.ex01_joins

import exposed.shared.mapping.OrderSchema.UserTable
import exposed.shared.mapping.OrderSchema.withOrdersTables
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex05_Self_Join: JdbcExposedTestBase() {

    companion object: KLogging()

    private fun assertUsers(id: Long, userName: String?, parentId: Long? = null) {
        id shouldBeEqualTo 2L
        userName shouldBeEqualTo "Barney"
        parentId shouldBeEqualTo null
    }

    /**
     * Inner join for self referenced table
     * 자식 엔티티 (id=4)의 부모 엔티티를 조회한다.
     *
     * ```sql
     * -- Postgres
     * SELECT u1.id,
     *        u1.user_name,
     *        u1.parent_id
     *   FROM users u1
     *      INNER JOIN users u2 ON (u1.id = u2.parent_id)
     *  WHERE u2.id = 4
     * ```
     * ```sql
     * -- MYSQL V8
     * SELECT u1.id,
     *        u1.user_name,
     *        u1.parent_id
     *   FROM users u1
     *      INNER JOIN users u2 ON (u1.id = u2.parent_id)
     *   WHERE u2.id = 4
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `self join`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MARIADB }

        withOrdersTables(testDB) { _, _, _, _, users ->
            val u1: Alias<UserTable> = users.alias("u1")
            val u2: Alias<UserTable> = users.alias("u2")

            val join: Join = u1
                .innerJoin(u2) { u1[users.id] eq u2[users.parentId] }

            val rows = join
                .select(
                    u1[users.id],
                    u1[users.userName],
                    u1[users.parentId],
                )
                .where { u2[users.id] eq 4L }
                .toList()

            rows shouldHaveSize 1
            assertUsers(
                rows[0][u1[users.id]].value,
                rows[0][u1[users.userName]],
                rows[0][u1[users.parentId]]?.value
            )
        }
    }

    /**
     * Inner join for self referenced table with alias
     * 자식 엔티티 (id=4)의 부모 엔티티를 조회한다.
     *
     * ```sql
     * -- Postgres
     * SELECT users.id,
     *        users.user_name,
     *        users.parent_id
     *   FROM users
     *      INNER JOIN users u2 ON (users.id = u2.parent_id)
     *  WHERE u2.id = 4
     * ```
     * ```sql
     * -- MySQL V8
     * SELECT users.id,
     *        users.user_name,
     *        users.parent_id
     *   FROM users
     *      INNER JOIN users u2 ON (users.id = u2.parent_id)
     *  WHERE u2.id = 4
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `self join with new alias`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MARIADB }

        withOrdersTables(testDB) { _, _, _, _, users ->
            val u2: Alias<UserTable> = users.alias("u2")

            val join: Join = users.innerJoin(u2) { users.id eq u2[users.parentId] }

            val rows = join
                .select(
                    users.id,
                    users.userName,
                    users.parentId,
                )
                .where { u2[users.id] eq 4L }
                .toList()

            rows shouldHaveSize 1
            assertUsers(
                rows[0][users.id].value,
                rows[0][users.userName],
                rows[0][users.parentId]?.value
            )
        }
    }
}
