package exposed.examples.jpa.ex04_relations.ex04_many_to_many

import exposed.examples.jpa.ex04_relations.ex04_many_to_many.MemberSchema.Group
import exposed.examples.jpa.ex04_relations.ex04_many_to_many.MemberSchema.GroupTable
import exposed.examples.jpa.ex04_relations.ex04_many_to_many.MemberSchema.MemberTable
import exposed.examples.jpa.ex04_relations.ex04_many_to_many.MemberSchema.User
import exposed.examples.jpa.ex04_relations.ex04_many_to_many.MemberSchema.UserStatus
import exposed.examples.jpa.ex04_relations.ex04_many_to_many.MemberSchema.UserTable
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withSuspendedTables
import exposed.shared.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex02_ManyToMany_Member: AbstractExposedTest() {

    companion object: KLogging()

    /**
     *
     * Postgres:
     * ```sql
     * SELECT "User".id,
     *        "User".first_name,
     *        "User".last_name,
     *        "User".username,
     *        "User".status, "User".created_at
     *   FROM "User"
    WHERE "User".first_name = 'Alice'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `generate schema`(testDB: TestDB) {
        withTables(testDB, *MemberSchema.memberTables) {
            log.info { "Schema generated" }

            // 현재는 데이터가 없으므로 빈 리스트가 반환된다.
            val users = UserTable.selectAll()
                .where { UserTable.firstName eq "Alice" }
                .toList()
            users.shouldBeEmpty()
        }
    }

    /**
     * ```sql
     * -- Postgres
     * INSERT INTO "User" (username, first_name, last_name, status)
     * VALUES ('gidget.kautzer', 'Kareem', 'Bogisich', 'INACTIVE')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `coroutine support`(testDB: TestDB) = runSuspendIO {
        withSuspendedTables(testDB, *MemberSchema.memberTables) {
            val prevCount = User.all().count()

            // rollback()을 호출하면 transaction은 롤백된다.
            User.new {
                username = faker.internet().username()
                firstName = faker.name().firstName()
                lastName = faker.name().lastName()
                status = UserStatus.ACTIVE
            }

            newSuspendedTransaction {
                User.new {
                    username = faker.internet().username()
                    firstName = faker.name().firstName()
                    lastName = faker.name().lastName()
                    status = UserStatus.INACTIVE
                }
            }
            rollback()  // 내부의 transaction은 실행되고, 외부의 transaction은 롤백된다.

            User.all().forEach {
                log.debug { "User: $it" } // User: User(id=xxxx, username=xxxx, status=INACTIVE)
            }
            val currentCount = User.all().count()
            currentCount shouldBeEqualTo prevCount + 1L  // 내부 Tx은 실행되고, 외부 Tx는 롤백된다.
        }
    }

    /**
     * ```sql
     * SELECT DISTINCT "Group".ID,
     *        "Group"."name",
     *        "Group".DESCRIPTION,
     *        "Group".CREATED_AT,
     *        "Group".OWNER_ID
     *   FROM "Group" INNER JOIN "User" ON "User".ID = "Group".OWNER_ID
     *                INNER JOIN "Member" ON "Group".ID = "Member".GROUP_ID AND "User".ID = "Member".USER_ID
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SQL DSL 로부터 DAO Entity 만들기`(testDB: TestDB) {
        withTables(testDB, *MemberSchema.memberTables) {

            createSample()

            /**
             * ```sql
             * -- Postgres
             * SELECT DISTINCT "Group".id,
             *                 "Group"."name",
             *                 "Group".description,
             *                 "Group".created_at,
             *                 "Group".owner_id
             *  FROM "Group" INNER JOIN "User" ON "User".id = "Group".owner_id
             *               INNER JOIN "Member" ON "Group".id = "Member".group_id AND "User".id = "Member".user_id
             * ```
             */
            val query: Query = GroupTable
                .innerJoin(UserTable)
                .innerJoin(MemberTable)
                .select(GroupTable.columns)
                .withDistinct()

            // Exposed SQL DSL로부터 DAO Entity 만들기
            val groups: List<Group> = Group.wrapRows(query).toList()

            groups.forEach {
                log.debug { "Group: $it" }
            }
        }
    }

    private fun Transaction.createSample() {
        val user1 = User.new {
            username = faker.internet().username()
            firstName = faker.name().firstName()
            lastName = faker.name().lastName()
            status = UserStatus.ACTIVE
        }

        val user2 = User.new {
            username = faker.internet().username()
            firstName = faker.name().firstName()
            lastName = faker.name().lastName()
            status = UserStatus.ACTIVE
        }

        val user3 = User.new {
            username = faker.internet().username()
            firstName = faker.name().firstName()
            lastName = faker.name().lastName()
            status = UserStatus.ACTIVE
        }

        val group1 = Group.new {
            name = faker.company().name()
            description = faker.lorem().sentence()
            owner = user1
        }

        val group2 = Group.new {
            name = faker.company().name()
            description = faker.lorem().sentence()
            owner = user2
        }

        // MemberTable 로 매핑을 하는 방식도 있고,
        // 이렇게 Entity 에 직접 설정하는 경우도 있다 (이 경우는 기존 매핑 중 필요 없는 것을 삭제하고 추가하는 방식이다)

        /**
         * ```sql
         * -- Postgres
         * DELETE FROM "Member" WHERE ("Member".group_id = 1) AND ("Member".user_id NOT IN (1, 2))
         * INSERT INTO "Member" (group_id, user_id) VALUES (1, 1)
         * INSERT INTO "Member" (group_id, user_id) VALUES (1, 2)
         * ```
         */
        group1.members = SizedCollection(listOf(user1, user2))

        /**
         * ```sql
         * DELETE FROM "Member" WHERE ("Member".group_id = 2) AND ("Member".user_id NOT IN (2, 3))
         * INSERT INTO "Member" (group_id, user_id) VALUES (2, 2)
         * INSERT INTO "Member" (group_id, user_id) VALUES (2, 3)
         * ```
         */
        group2.members = SizedCollection(listOf(user2, user3))

        commit()
    }

}
