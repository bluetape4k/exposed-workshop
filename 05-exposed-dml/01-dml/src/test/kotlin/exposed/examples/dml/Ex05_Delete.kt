package exposed.examples.dml

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.currentTestDB
import exposed.shared.tests.expectException
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.like
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.joinQuery
import org.jetbrains.exposed.v1.core.lastQueryAlias
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.jdbc.delete
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteIgnoreWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex05_Delete: AbstractExposedTest() {

    companion object: KLogging()

    private val limitNotSupported = TestDB.ALL_POSTGRES

    /**
     * [deleteWhere], [deleteIgnoreWhere] 사용 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, userData ->
            userData.deleteAll()
            userData.selectAll().count() shouldBeEqualTo 0L

            val expr = users.name like "%thing"

            val smthId = users.select(users.id).where(expr).single()[users.id]
            smthId shouldBeEqualTo "smth"

            // DELETE FROM Users WHERE Users.`name` LIKE '%thing'
            users.deleteWhere { expr } shouldBeEqualTo 1

            val hasSmth = users.select(users.id).where(expr).any()
            hasSmth.shouldBeFalse()

            if (currentDialectTest is MysqlDialect) {
                val cityOne = cities.id eq 1
                cities.selectAll().where(cityOne).count().toInt() shouldBeEqualTo 1
                expectException<ExposedSQLException> {
                    // Users가 Cities를 참조합니다. 해당 City를 참조하는 User를 먼저 삭제해야 City를 삭제할 수 있습니다.
                    cities.deleteWhere { cityOne }
                }

                // User에 의해 참조되어 있으므로, 삭제는 실패한다. 
                // DELETE IGNORE FROM Cities WHERE Cities.city_id = 1
                cities.deleteIgnoreWhere { cityOne } shouldBeEqualTo 0
                cities.selectAll().where { cityOne }.count() shouldBeEqualTo 1L
            }
        }
    }

    /**
     * [deleteAll] 을 이용해 테이블의 모든 행을 삭제합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete table in context`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, userData ->
            userData.selectAll().shouldNotBeEmpty()

            // DELETE FROM userdata
            userData.deleteAll()
            userData.selectAll().any().shouldBeFalse()  // UserData 에는 아무 것도 없다

            val smthId = users
                .select(users.id)
                .where { users.name like "%thing" }
                .single()[users.id]

            smthId shouldBeEqualTo "smth"

            // DELETE FROM users WHERE users."name" LIKE '%thing'
            users.deleteWhere { users.name like "%thing" }

            users.selectAll()
                .where { users.name.like("%thing") }
                .any().shouldBeFalse()                        // name like %thing 에 해당하는 행은 없다
        }
    }

    /**
     * ### [deleteWhere] with limit
     *
     * [deleteWhere] 함수에 limit 을 적용하면, 삭제 대상 행의 수를 제한할 수 있습니다.
     *
     * ```sql
     * -- Postgres
     * DELETE FROM USERDATA
     *  WHERE USERDATA."value" = 20
     *  LIMIT 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete with limit`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, _, userData ->
            if (currentTestDB in limitNotSupported) {
                expectException<UnsupportedByDialectException> {
                    userData.deleteWhere(limit = 1) { userData.value eq 20 }
                }
            } else {
                userData.selectAll()
                    .where { userData.value eq 20 }
                    .count() shouldBeEqualTo 2L

                // 최대 1개만 삭제한다.
                userData.deleteWhere(limit = 1) { userData.value eq 20 }

                userData.selectAll()
                    .where { userData.value eq 20 }
                    .count() shouldBeEqualTo 1L
            }
        }
    }


    /**
     * ### Delete with single join
     *
     * `Users` 테이블과 `UserData` 테이블을 조인하여, `UserData` 테이블의 일부 행을 삭제합니다.
     *
     * ```sql
     * -- Postgres
     * DELETE
     *   FROM userdata USING users
     *  WHERE users.id = userdata.user_id
     *    AND userdata.user_id LIKE '%ey'
     * ```
     * ```sql
     * -- MySQL V8
     * DELETE UserData
     *   FROM Users INNER JOIN UserData ON Users.id = UserData.user_id
     *  WHERE UserData.user_id LIKE '%ey'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete with single join`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, userData ->
            val join = users innerJoin userData

            val query1 = join.selectAll().where { userData.userId like "%ey" }
            query1.count() shouldBeGreaterThan 0L  // 행이 존재한다.

            join.delete(userData) { userData.userId like "%ey" } shouldBeEqualTo 1
            query1.count() shouldBeEqualTo 0L   // 삭제되었다.

            val query2 = join.selectAll()
            query2.count() shouldBeGreaterThan 0L

            join.delete(userData) shouldBeGreaterThan 0
            query2.count() shouldBeEqualTo 0L
        }
    }

    /**
     * ### Delete with multiple alias joins
     *
     * ```sql
     * -- Postgres
     * DELETE FROM userdata stats
     *  USING cities towns, users people
     *  WHERE towns.city_id = people.city_id AND people.id = stats.user_id
     *    AND towns."name" = 'Munich'
     * ```
     * ```sql
     * -- MySQL V8
     * DELETE stats
     *   FROM Cities towns
     *      INNER JOIN Users people ON towns.city_id = people.city_id
     *      INNER JOIN UserData stats ON people.id = stats.user_id
     *  WHERE towns.`name` = 'Munich'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete with multiple alias joins`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withCitiesAndUsers(testDB) { cities, users, userData ->
            val towns = cities.alias("towns")
            val people = users.alias("people")
            val stats = userData.alias("stats")

            val aliasedJoin = Join(towns)
                .innerJoin(people, { towns[cities.id] }, { people[users.cityId] })
                .innerJoin(stats, { people[users.id] }, { stats[userData.userId] })

            val query = aliasedJoin
                .selectAll()
                .where { towns[cities.name] eq "Munich" }

            query.count() shouldBeGreaterThan 0L

            // 행을 삭제한다.
            aliasedJoin.delete(stats) { towns[cities.name] eq "Munich" } shouldBeGreaterThan 0

            query.count() shouldBeEqualTo 0L
        }
    }

    /**
     * ### Delete with join subQuery ([joinQuery])
     *
     * ```sql
     * -- Postgres
     * DELETE FROM userdata
     *  USING (SELECT users.id,
     *                users."name"
     *           FROM users
     *          WHERE users.city_id = 2
     *        ) q0
     *  WHERE (userdata.user_id = q0.id)
     *    AND q0."name" LIKE '%ey'
     * ```
     * ```sql
     * -- MySQL V8
     * DELETE UserData
     *   FROM UserData INNER JOIN (
     *      SELECT Users.id,
     *             Users.`name`
     *        FROM Users
     *       WHERE Users.city_id = 2
     *   ) q0 ON  (UserData.user_id = q0.id)
     *  WHERE q0.`name` LIKE '%ey'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete with join query`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        withCitiesAndUsers(testDB) { _, users, userData ->
            // subQuery 를 이용한 join
            val singleJoinQuery = userData.joinQuery(
                on = { userData.userId eq it[users.id] },
                joinPart = { users.select(users.id, users.name).where { users.cityId eq 2 } }
            )

            val joinCount = singleJoinQuery.selectAll().count()
            joinCount shouldBeGreaterThan 0

            val joinCountWithCondition = singleJoinQuery.selectAll()
                .where {
                    singleJoinQuery.lastQueryAlias!![users.name] like "%ey"
                }
                .count()

            joinCountWithCondition shouldBeGreaterThan 0L

            singleJoinQuery.delete(userData) {
                singleJoinQuery.lastQueryAlias!![users.name] like "%ey"
            } shouldBeEqualTo joinCountWithCondition.toInt()

            singleJoinQuery.selectAll().count() shouldBeEqualTo joinCount - joinCountWithCondition
        }
    }

    /**
     * ### `delete(table, ignore = true) 를 이용하여 DELETE 수행 시 예외를 무시할 수 있다.
     *
     * `delete(table, ignore = true) { ... }` 와 같이 `ignore = true` 를 지정하여, 참조 위배 등의 예외를 무시할 수 있다.
     *
     * 같은 기능으로 [deleteIgnoreWhere] 를 사용할 수 있다.
     *
     * 단, MySQL, MariaDB 에서만 지원한다.
     *
     * ```sql
     * -- MySQL V8
     * DELETE IGNORE Users, UserData
     *   FROM Users INNER JOIN UserData ON Users.id = UserData.user_id
     *  WHERE Users.id = 'smth'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete ignore with join`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB }

        withCitiesAndUsers(testDB) { _, users, userData ->
            val join = users innerJoin userData
            val query = join.selectAll().where { userData.userId eq "smth" }
            query.count() shouldBeGreaterThan 0L

            expectException<ExposedSQLException> {
                // UserData 테이블은 Users 테이블을 참조하고 있기 때문에, Users 테이블을 먼저 삭제할 수 없습니다.
                join.delete(users, userData) { users.id eq "smth" }
            }

            // `ignore=true` 를 지정하여, UserData 참조 관련 에러는 무시된다.
            // User 테이블은 건너뛰고 UserData 테이블의 행을 삭제된다.
            join.delete(users, userData, ignore = true) { users.id eq "smth" } shouldBeEqualTo 2

            query.count().toInt() shouldBeEqualTo 0
        }
    }
}
