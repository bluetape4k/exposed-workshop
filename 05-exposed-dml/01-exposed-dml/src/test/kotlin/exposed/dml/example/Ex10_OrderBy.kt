package exposed.dml.example

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SortOrder.ASC_NULLS_FIRST
import org.jetbrains.exposed.sql.SortOrder.ASC_NULLS_LAST
import org.jetbrains.exposed.sql.SortOrder.DESC_NULLS_FIRST
import org.jetbrains.exposed.sql.SortOrder.DESC_NULLS_LAST
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.substring
import org.jetbrains.exposed.sql.vendors.H2Dialect
import org.jetbrains.exposed.sql.vendors.H2Dialect.H2CompatibilityMode
import org.jetbrains.exposed.sql.vendors.OracleDialect
import org.jetbrains.exposed.sql.vendors.PostgreSQLDialect
import org.jetbrains.exposed.sql.vendors.h2Mode
import org.jetbrains.exposed.sql.wrapAsExpression
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertContentEquals

class Ex10_OrderBy: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * users 테이블의 `id` 컬럼을 기준으로 오름차순 정렬합니다.
     *
     * ```sql
     * -- PostgreSQL
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  ORDER BY users.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `orderBy 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll().orderBy(users.id).toList()

            rows shouldHaveSize 5
            rows.map { it[users.id] } shouldBeEqualTo listOf("alex", "andrey", "eugene", "sergey", "smth")
        }
    }

    /**
     * DB 마다 NULL 값을 처음에 정렬할지 마지막에 정렬할지 다릅니다.
     */
    private fun isNullFirst(): Boolean = when (currentDialectTest) {
        is OracleDialect, is PostgreSQLDialect -> true
        is H2Dialect ->
            currentDialectTest.h2Mode in listOf(
                H2CompatibilityMode.PostgreSQL,
                H2CompatibilityMode.Oracle
            )

        else -> false
    }

    /**
     * Users 테이블의 `city_id` 컬럼을 기준으로 내림차순 정렬하고, 그 다음 `id` 컬럼을 기준으로 오름차순 정렬합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  ORDER BY users.city_id DESC, users.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `orderBy 02`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll()
                .orderBy(users.cityId, SortOrder.DESC)
                .orderBy(users.id)
                .toList()

            rows shouldHaveSize 5
            val usersWithoutCities = listOf("alex", "smth")
            val otherUsers = listOf("eugene", "sergey", "andrey")
            val expected = if (isNullFirst()) {
                usersWithoutCities + otherUsers
            } else {
                otherUsers + usersWithoutCities
            }
            expected.forEachIndexed { index, expect ->
                rows[index][users.id] shouldBeEqualTo expect
            }
        }
    }

    /**
     * Users 테이블의 `city_id` 컬럼을 기준으로 내림차순 정렬하고, 그 다음 `id` 컬럼을 기준으로 오름차순 정렬합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  ORDER BY users.city_id DESC, users.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `orderBy 03`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll()
                .orderBy(users.cityId to SortOrder.DESC, users.id to SortOrder.ASC)
                .toList()

            rows shouldHaveSize 5
            val usersWithoutCities = listOf("alex", "smth")
            val otherUsers = listOf("eugene", "sergey", "andrey")
            val expected = if (isNullFirst()) {
                usersWithoutCities + otherUsers
            } else {
                otherUsers + usersWithoutCities
            }
            expected.forEachIndexed { index, expect ->
                rows[index][users.id] shouldBeEqualTo expect
            }
        }
    }

    /**
     * `orderBy` 함수와 [groupBy] 함수를 함께 사용하는 예
     *
     * ```sql
     * -- Postgres
     * SELECT cities."name", COUNT(users.id)
     *   FROM cities INNER JOIN users ON cities.city_id = users.city_id
     *  GROUP BY cities."name"
     *  ORDER BY cities."name" ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `orderBy 04`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val r = (cities innerJoin users)
                .select(
                    cities.name,
                    users.id.count()
                )
                .groupBy(cities.name)
                .orderBy(cities.name)  // ASC 가 기본값
                .toList()

            r shouldHaveSize 2
            r[0][cities.name] shouldBeEqualTo "Munich"
            r[0][users.id.count()] shouldBeEqualTo 2
            r[1][cities.name] shouldBeEqualTo "St. Petersburg"
            r[1][users.id.count()] shouldBeEqualTo 1
        }
    }

    /**
     * Expression을 사용하여 정렬하는 예
     *
     * ```sql
     * -- Postgres
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  ORDER BY SUBSTRING(users.id, 2, 1) ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `orderBy 06`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val orderByExpression = users.id.substring(2, 1)
            val rows = users.selectAll()
                .orderBy(orderByExpression to SortOrder.ASC)
                .toList()

            rows shouldHaveSize 5
            rows.map { it[users.id] } shouldBeEqualTo listOf("sergey", "alex", "smth", "andrey", "eugene")
        }
    }

    /**
     * SubQuery 를 Expression으로 사용하여 정렬하는 예
     *
     * [wrapAsExpression]을 사용하여 subquery를 expression으로 사용할 수 있다.
     *
     * ```sql
     * -- Postgres
     * SELECT cities.city_id,
     *        cities."name"
     *   FROM cities
     *  ORDER BY (SELECT COUNT(users.id)
     *              FROM users
     *             WHERE cities.city_id = users.city_id
     *           ) DESC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `orderBy Expressions`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            // NOTE: wrapAsExpression 은 subquery를 wrap해서 expression으로 사용할 수 있다.
            val expression = wrapAsExpression<Int>(
                users
                    .select(users.id.count())
                    .where { cities.id eq users.cityId }
            )

            val result = cities
                .selectAll()
                .orderBy(expression, SortOrder.DESC)
                .map { it[cities.name] }

            // Munich - 2 users
            // St. Petersburg - 1 user
            // Prague - 0 users
            result shouldBeEqualTo listOf("Munich", "St. Petersburg", "Prague")
        }
    }

    /**
     * 정렬 시, NULL 값의 위치를 지정하는 예
     *
     * OrderBy with
     *  [SortOrder.ASC_NULLS_FIRST],
     *  [SortOrder.ASC_NULLS_LAST],
     *  [SortOrder.DESC_NULLS_FIRST],
     *  [SortOrder.DESC_NULLS_LAST]
     *
     * ```sql
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  ORDER BY users.city_id ASC NULLS FIRST,
     *           users.id ASC;
     *
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  ORDER BY users.city_id ASC NULLS LAST,
     *           users.id ASC;
     *
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  ORDER BY users.city_id DESC NULLS FIRST,
     *           users.id ASC;
     *
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  ORDER BY users.city_id DESC NULLS LAST,
     *           users.id ASC;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `orderBy NullsFirst`(testDB: TestDB) {
        // city IDs null, user IDs sorted ascending
        val usersWithoutCities = listOf("alex", "smth")
        // city IDs sorted descending, user IDs sorted ascending
        val otherUsers = listOf("eugene", "sergey", "andrey")
        // city IDs sorted ascending, user IDs sorted ascending
        val otherUsersAsc = listOf("andrey", "eugene", "sergey")

        val cases = listOf(
            SortOrder.ASC_NULLS_FIRST to usersWithoutCities + otherUsersAsc,
            SortOrder.ASC_NULLS_LAST to otherUsersAsc + usersWithoutCities,
            SortOrder.DESC_NULLS_FIRST to usersWithoutCities + otherUsers,
            SortOrder.DESC_NULLS_LAST to otherUsers + usersWithoutCities,
        )
        withCitiesAndUsers(testDB) { _, users, _ ->
            cases.forEach { (sortOrder, expected) ->
                val rows = users.selectAll()
                    .orderBy(
                        users.cityId to sortOrder,
                        users.id to SortOrder.ASC
                    )
                    .toList()

                rows shouldHaveSize 5
                expected.forEachIndexed { index, expect ->
                    rows[index][users.id] shouldBeEqualTo expect
                }
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS nullablestrings (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NULL
     * )
     * ```
     */
    object NullableStrings: Table() {
        val id: Column<Int> = integer("id").autoIncrement()
        val name: Column<String?> = varchar("name", 50).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * Nullable String 에 대한 정렬 예 (NULLS FIRST, NULLS LAST)
     *
     * ```sql
     * SELECT nullablestrings.id
     *   FROM nullablestrings
     *  ORDER BY nullablestrings."name" DESC NULLS LAST;
     *
     * SELECT nullablestrings.id
     *   FROM nullablestrings
     *  ORDER BY nullablestrings."name" ASC NULLS LAST;
     *
     * SELECT nullablestrings.id
     *   FROM nullablestrings
     *  ORDER BY nullablestrings."name" DESC NULLS FIRST;
     *
     * SELECT nullablestrings.id
     *   FROM nullablestrings
     *  ORDER BY nullablestrings."name" ASC NULLS FIRST;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Nullable String Ordering`(testDB: TestDB) {
        withTables(testDB, NullableStrings) {
            NullableStrings.insert {
                it[name] = "a"
            }
            NullableStrings.insert {
                it[name] = "b"
            }
            NullableStrings.insert {
                it[name] = null
            }
            NullableStrings.insert {
                it[name] = "c"
            }

            fun assertOrdered(expected: List<Int>, order: SortOrder) {
                val ordered = NullableStrings
                    .select(NullableStrings.id)
                    .orderBy(NullableStrings.name, order)
                    .map { it[NullableStrings.id] }

                ordered shouldBeEqualTo expected
                assertContentEquals(expected, ordered)
            }
            assertOrdered(listOf(4, 2, 1, 3), DESC_NULLS_LAST) // c, b, a, null
            assertOrdered(listOf(1, 2, 4, 3), ASC_NULLS_LAST) // a, b, c, null
            assertOrdered(listOf(3, 4, 2, 1), DESC_NULLS_FIRST) // null, c, b, a
            assertOrdered(listOf(3, 1, 2, 4), ASC_NULLS_FIRST) // null, a, b, c
        }
    }
}
