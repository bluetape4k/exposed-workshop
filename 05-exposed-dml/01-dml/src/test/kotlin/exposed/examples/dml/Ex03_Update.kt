package exposed.examples.dml

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.expectException
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.joinQuery
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex03_Update: AbstractExposedTest() {

    companion object: KLogging()

    private val limitNotSupported = TestDB.ALL_POSTGRES


    /**
     * Update 예제
     *
     * ```sql
     * -- Postgres
     * UPDATE users
     *    SET "name"='Alexey'
     *  WHERE users.id = 'alex'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val alexId = "alex"
            val alexName = users
                .select(users.name)
                .where { users.id eq alexId }
                .single()[users.name]
            alexName shouldBeEqualTo "Alex"

            val newName = "Alexey"
            users.update({ users.id eq alexId }) {
                it[users.name] = newName
            }

            val alexUpdatedName = users
                .select(users.name)
                .where { users.id eq alexId }
                .first()[users.name]

            alexUpdatedName shouldBeEqualTo newName
        }
    }


    /**
     * Update 에 LIMIT 적용하기 (Postgres 는 지원하지 않습니다)
     *
     * ```sql
     * -- MySQL V8
     * UPDATE Users
     *    SET id='NewName'
     *  WHERE Users.id LIKE 'a%'
     *  LIMIT 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update with limit`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            if (testDB in limitNotSupported) {
                expectException<UnsupportedByDialectException> {
                    users.update({ users.id like "a%" }, limit = 1) {
                        it[users.id] = "NewName"
                    }
                }
            } else {
                val aNames = users
                    .select(users.name)
                    .where { users.id like "a%" }
                    .map { it[users.name] }
                aNames.size shouldBeEqualTo 2

                users.update({ users.id like "a%" }, limit = 1) {
                    it[users.id] = "NewName"
                }

                val unchanged = users
                    .select(users.name)
                    .where { users.id like "a%" }
                    .count().toInt()

                val changed = users
                    .select(users.name)
                    .where { users.id eq "NewName" }
                    .count().toInt()

                unchanged shouldBeEqualTo 1
                changed shouldBeEqualTo 1
            }
        }
    }


    /**
     * 테이블들을 Join 하여 Update 하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update with single join`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, userData ->
            /**
             * Users 와 UserData 테이블을 Inner Join 하여 Update 하기
             * ```sql
             * -- Postgres
             * UPDATE userdata
             *    SET "comment"=users."name",
             *        "value"=123
             *   FROM users
             *  WHERE users.id = userdata.user_id
             * ```
             */
            val join = users.innerJoin(userData)
            join.update {
                it[userData.comment] = users.name
                it[userData.value] = 123
            }

            join.selectAll().forEach {
                it[userData.comment] shouldBeEqualTo it[users.name]
                it[userData.value] shouldBeEqualTo 123
            }

            /**
             * 명시적으로 Join 조건을 지정
             *
             * ```sql
             * -- Postgres
             * UPDATE userdata
             *    SET "comment"=users."name",
             *        "value"=0
             *   FROM users
             *  WHERE users.id = userdata.user_id
             *    AND  (users.id = 'smth')
             * ```
             */
            val joinWithConstraint = users.innerJoin(userData, { users.id }, { userData.userId }) {
                users.id eq "smth"
            }
            joinWithConstraint.update {
                it[userData.comment] = users.name
                it[userData.value] = 0
            }

            joinWithConstraint.selectAll().forEach {
                it[userData.comment] shouldBeEqualTo it[users.name]
                it[userData.value] shouldBeEqualTo 0
            }
        }
    }

    /**
     * 테이블들을 Join 한 Update 문에 LIMIT 적용하기
     * 단, MariaDB, Oracle, SQLServer에서만 지원합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update with join and limit`(testDB: TestDB) {
        // val supportsUpdateWithJoinAndLimit = TestDB.ALL_MARIADB + TestDB.ORACLE + TestDB.SQLSERVER
        // Assumptions.assumeTrue(testDB !in supportsUpdateWithJoinAndLimit)
        Assumptions.assumeTrue { testDB in TestDB.ALL_MARIADB }

        withCitiesAndUsers(testDB) { _, users, userData ->
            val join = users.innerJoin(userData)

            val maxToUpdate = 2
            join.selectAll().count() shouldBeEqualTo 4L

            val updatedValue = 123
            val valueQuery = join.selectAll().where { userData.value eq updatedValue }
            valueQuery.count() shouldBeEqualTo 0L

            /**
             * ```sql
             * -- MariaDB
             * UPDATE Users INNER JOIN UserData ON Users.id = UserData.user_id
             *    SET UserData.`value`=123
             *        Users.flags=1
             *  LIMIT 2
             * ```
             */
            join.update(limit = maxToUpdate) {
                it[userData.value] = updatedValue
                it[users.flags] = 1
            }

            valueQuery.count().toInt() shouldBeEqualTo maxToUpdate
        }
    }


    /**
     * 다중 조인을 사용한 Update 문
     *
     * ```sql
     * -- Postgres
     * UPDATE userdata
     *    SET "comment"=users."name",
     *        "value"=123
     *   FROM cities, users
     *  WHERE cities.city_id = users.city_id
     *    AND users.id = userdata.user_id
     * ```
     *
     * ```sql
     * -- MySQL
     * UPDATE Cities
     *          INNER JOIN Users ON Cities.city_id = Users.city_id
     *          INNER JOIN UserData ON Users.id = UserData.user_id
     *    SET UserData.comment=Users.`name`,
     *        UserData.`value`=123
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update with multiple joins`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withCitiesAndUsers(testDB) { cities, users, userData ->
            val join = cities.innerJoin(users).innerJoin(userData)
            join.update {
                it[userData.comment] = users.name
                it[userData.value] = 123
            }

            join.selectAll().forEach {
                it[userData.comment] shouldBeEqualTo it[users.name]
                it[userData.value] shouldBeEqualTo 123
            }
        }
    }


    /**
     * 다중 테이블 조인과 Where 조건을 추가한 Update
     *
     * ```sql
     * -- Postgres
     * UPDATE table_b
     *    SET bar='baz'
     *   FROM table_a
     *  WHERE table_a.id = table_b.table_a_id
     *    AND table_a.foo = 'foo';
     *
     * UPDATE table_b
     *    SET bar='baz'
     *   FROM table_a
     *  WHERE table_a.id = table_b.table_a_id
     *    AND (table_b.bar = 'foo')
     *    AND table_a.foo = 'foo';
     * ```
     *
     * ```sql
     * -- MySQL V8
     * UPDATE table_a INNER JOIN table_b ON table_a.id = table_b.table_a_id
     *    SET table_b.bar='baz'
     *  WHERE table_a.foo = 'foo';
     *
     *  UPDATE table_a INNER JOIN table_b
     *      ON table_a.id = table_b.table_a_id AND  (table_b.bar = 'foo')
     *     SET table_b.bar='baz'
     *   WHERE table_a.foo = 'foo'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update with join and where`(testDB: TestDB) {
        val supportWhereDb = TestDB.ALL - TestDB.ALL_H2

        val tableA = object: LongIdTable("table_a") {
            val foo = varchar("foo", 255)
        }
        val tableB = object: LongIdTable("table_b") {
            val bar = varchar("bar", 255)
            val tableAId = reference("table_a_id", tableA)
        }

        withTables(testDB, tableA, tableB) {
            val aId = tableA.insertAndGetId { it[foo] = "foo" }
            tableB.insert {
                it[bar] = "zip"
                it[tableAId] = aId
            }

            val join = tableA.innerJoin(tableB)

            if (testDB in supportWhereDb) {
                join.update({ tableA.foo eq "foo" }) {
                    it[tableB.bar] = "baz"
                }
                join.selectAll().single().also {
                    it[tableB.bar] shouldBeEqualTo "baz"
                }

                val joinWithConstraint = tableA.innerJoin(tableB, { tableA.id }, { tableB.tableAId }) {
                    tableB.bar eq "foo"
                }
                joinWithConstraint.update({ tableA.foo eq "foo" }) {
                    it[tableB.bar] = "baz"
                }
                joinWithConstraint.selectAll().count().toInt() shouldBeEqualTo 0

            } else {
                expectException<UnsupportedByDialectException> {
                    join.update({ tableA.foo eq "foo" }) {
                        it[tableB.bar] = "baz"
                    }
                }
            }
        }
    }


    /**
     * Subquery 를 이용한 JOIN 문을 사용한 Update
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update with join query`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withCitiesAndUsers(testDB) { _, users, userData ->
            /**
             * single join query using join()
             *
             * ```sql
             * -- Postgres
             * UPDATE userdata
             *    SET "value"=123
             *   FROM (SELECT users.id, users."name", users.city_id, users.flags
             *           FROM users
             *          WHERE users.city_id = 1
             *        ) u2
             *  WHERE userdata.user_id = u2.id
             * ```
             */
            val userAlias = users.selectAll().where { users.cityId eq 1 }.alias("u2")
            val joinWithSubQuery = userData.innerJoin(userAlias, { userData.userId }, { userAlias[users.id] })
            joinWithSubQuery.update {
                it[userData.value] = 123
            }

            joinWithSubQuery.selectAll().all { it[userData.value] == 123 }.shouldBeTrue()

            // does not support either multi-table joins or update(where)
            Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

            /**
             * single join query using join() with update(where)
             *
             * ```sql
             * -- Postgres
             * UPDATE userdata
             *    SET "value"=42
             *   FROM (SELECT users.id,
             *                users."name",
             *                users.city_id,
             *                users.flags
             *           FROM users
             *          WHERE users.city_id = 1
             *        ) u2
             *   WHERE userdata.user_id = u2.id
             *     AND userdata."comment" LIKE 'Comment%'
             * ```
             */
            joinWithSubQuery.update({ userData.comment like "Comment%" }) {
                it[userData.value] = 42
            }

            joinWithSubQuery.selectAll().forEach {
                it[userData.value] shouldBeEqualTo 42
            }

            /**
             * multiple join queries using [joinQuery]
             *
             * ```sql
             * -- Postgres
             * UPDATE userdata
             *    SET "value"=99
             *   FROM (SELECT users.id,
             *                users."name",
             *                users.city_id,
             *                users.flags
             *           FROM users
             *          WHERE users.city_id = 1
             *        ) q0,
             *        (SELECT users.id,
             *                users."name",
             *                users.city_id,
             *                users.flags
             *           FROM users
             *          WHERE users."name" LIKE '%ey'
             *        ) q1
             *  WHERE  (userdata.user_id = q0.id)
             *    AND  (userdata.user_id = q1.id)
             * ```
             * ```sql
             * -- MySQL V8
             * UPDATE UserData
             *      INNER JOIN (SELECT Users.id,
             *                         Users.`name`,
             *                         Users.city_id,
             *                         Users.flags
             *                    FROM Users
             *                   WHERE Users.city_id = 1) q0 ON  (UserData.user_id = q0.id)
             *      INNER JOIN (SELECT Users.id,
             *                         Users.`name`,
             *                         Users.city_id,
             *                         Users.flags
             *                    FROM Users
             *                   WHERE Users.`name` LIKE '%ey') q1 ON  (UserData.user_id = q1.id)
             *     SET UserData.`value`=99
             * ```
             */
            val singleJoinQuery = userData.joinQuery(
                on = { userData.userId eq it[users.id] },
                joinPart = { users.selectAll().where { users.cityId eq 1 } }
            )
            val doubleJoinQuery = singleJoinQuery.joinQuery(
                on = { userData.userId eq it[users.id] },
                joinPart = { users.selectAll().where { users.name like "%ey" } }
            )
            doubleJoinQuery.update {
                it[userData.value] = 99
            }

            doubleJoinQuery.selectAll().forEach {
                it[userData.value] shouldBeEqualTo 99
            }
        }
    }
}
