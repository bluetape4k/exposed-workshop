package exposed.examples.dml

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withSchemas
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex08_Count: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * [count] with `withDistinct` 예
     *
     * ```sql
     * -- Postgres
     * -- distinct with selectAll
     * SELECT COUNT(*)
     *   FROM (SELECT DISTINCT cities.city_id Cities_city_id,
     *                         cities."name" Cities_name,
     *                         users.id Users_id,
     *                         users."name" Users_name,
     *                         users.city_id Users_city_id,
     *                         users.flags Users_flags
     *                    FROM cities INNER JOIN users ON cities.city_id = users.city_id
     *        ) subquery
     * ```
     * ```sql
     * -- Postgres
     * -- distinct with cities.city_id, users.id
     * SELECT COUNT(*)
     *   FROM (SELECT DISTINCT cities.city_id Cities_city_id,
     *                         users.id Users_id
     *           FROM cities INNER JOIN users ON cities.city_id = users.city_id
     *        ) subquery
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count works with Query that contains distinct and columns with same name from different tables`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            cities.innerJoin(users)
                .selectAll()
                .withDistinct()
                .count() shouldBeEqualTo 3L

            cities.innerJoin(users)
                .select(cities.id, users.id)
                .withDistinct()
                .count() shouldBeEqualTo 3L
        }
    }

    /**
     * 특정 컬럼에 count 함수 적용하기
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(DISTINCT cities.city_id) FROM cities;
     * SELECT COUNT(users.id) FROM users
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `특정 컬럼의 count, countDistinct 함수 적용하기`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->

            // SELECT COUNT(DISTINCT cities.city_id) FROM cities;
            val cityCount = cities.id.countDistinct()
            cities.select(cityCount).single()[cityCount] shouldBeEqualTo 3L

            // SELECT COUNT(users.id) FROM users
            val userIdCount = users.id.count()
            users.select(userIdCount).single()[userIdCount] shouldBeEqualTo 5L
        }
    }

    /**
     * [count] with `withDistinct`, [count] with [groupBy] 예
     *
     * ```sql
     * -- count with distinct
     * SELECT COUNT(*)
     *   FROM (SELECT DISTINCT USERDATA.USER_ID UserData_user_id
     *           FROM USERDATA
     *        ) subquery
     * ```
     *
     * ```sql
     * -- count with group by
     * SELECT COUNT(*)
     *   FROM (SELECT MAX(USERDATA."value") exp0
     *           FROM USERDATA
     *          GROUP BY USERDATA.USER_ID
     *        ) subquery
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count returns right value for Query with group by`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, _, userData ->
            val uniqueUsersInData = userData
                .select(userData.userId)
                .withDistinct()
                .count()

            val sameQueryWithGrouping = userData
                .select(userData.value.max())
                .groupBy(userData.userId)
                .count()

            sameQueryWithGrouping shouldBeEqualTo uniqueUsersInData
        }
    }

    /**
     *
     * ```sql
     * SELECT COUNT(*)
     *   FROM (
     *          SELECT DISTINCT CUSTOM.TESTER.AMOUNT tester_amount
     *            FROM CUSTOM.TESTER
     *        ) subquery
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count alias with table schema`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        val custom = prepareSchemaForTest("custom")
        val tester = object: Table("custom.tester") {
            val amount = integer("amount")
        }

        withSchemas(testDB, custom) {
            SchemaUtils.create(tester)

            repeat(3) {
                tester.insert { it[amount] = 99 }
            }

            // count alias is generated for any query with distinct/groupBy/limit & throws if schema name included
            tester.select(tester.amount)
                .withDistinct()
                .count().toInt() shouldBeEqualTo 1

            SchemaUtils.drop(tester)
        }
    }

    /**
     * OFFSET, LIMIT 을 사용하는 조회 쿼리의 ROW 수를 COUNT 하는 예제
     *
     * ```sql
     * -- offset, limit 적용
     * SELECT COUNT(*)
     *   FROM (SELECT TESTER."value" tester_value
     *           FROM TESTER
     *          LIMIT 2
     *         OFFSET 1
     *        ) subquery
     * ```
     *
     * ```sql
     * -- limit 적용
     * SELECT COUNT(*)
     *   FROM (SELECT TESTER."value" tester_value
     *           FROM TESTER
     *          LIMIT 2
     *        ) subquery
     * ```
     *
     * ```sql
     * -- offset 적용
     * SELECT COUNT(*)
     *   FROM (SELECT TESTER."value" tester_value
     *           FROM TESTER
     *         OFFSET 2
     *        ) subquery
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count with offset and limit`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        val tester = object: Table("tester") {
            val value = integer("value")
        }

        withTables(testDB, tester) {
            tester.batchInsert(listOf(1, 2, 3, 4, 5)) {
                this[tester.value] = it
            }

            tester.selectAll().count() shouldBeEqualTo 5L
            tester.selectAll().offset(1).limit(2).count() shouldBeEqualTo 2L
            tester.selectAll().limit(2).count() shouldBeEqualTo 2L
            tester.selectAll().offset(2).count() shouldBeEqualTo 3L
        }
    }
}
