package exposed.examples.dml

import exposed.shared.dml.DMLTestData
import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEqualTo
import org.jetbrains.exposed.v1.core.ColumnSet
import org.jetbrains.exposed.v1.core.FieldSet
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andHaving
import org.jetbrains.exposed.v1.jdbc.orHaving
import org.jetbrains.exposed.v1.jdbc.orWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * 기존 Query 에 대한 변경이나 추가를 할 수 있는 `adjustSelect`, `adjustColumnSet`, `adjustWhere`, `adjustHaving` 등의 함수를 테스트한다.
 */
class Ex20_AdjustQuery: JdbcExposedTestBase() {

    companion object: KLogging()

    private val predicate = Op.build {
        (DMLTestData.Users.id eq "andrey") or (DMLTestData.Users.name eq "Sergey")
    }

    private fun Query.assertQueryResultValid() {
        val users = DMLTestData.Users
        val cities = DMLTestData.Cities

        this.forEach { row ->
            val userName = row[users.name]
            val cityName = row[cities.name]
            when (userName) {
                "Andrey" -> cityName shouldBeEqualTo "St. Petersburg"
                "Sergey" -> cityName shouldBeEqualTo "Munich"
                else -> error { "Unexpected user name: $userName" }

            }
        }
    }

    /**
     * `adjustSelect` 를 사용하면 기존 Query에서 조회할 컬럼을 변경할 수 있다.
     *
     * ```sql
     * -- Postgres
     * SELECT users."name", cities."name"
     *   FROM users INNER JOIN cities ON cities.city_id = users.city_id
     *  WHERE (users.id = 'andrey')
     *     OR (users."name" = 'Sergey')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `adjust query slice`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            // [users.name]
            val queryAdjusted: Query = (users innerJoin cities)
                .select(users.name)
                .where(predicate)

            fun Query.sliceIt(): FieldSet = this.set.source.select(users.name, cities.name).set

            // [users.name]
            val oldSlice = queryAdjusted.set.fields

            // [users.name, cities.name]
            val expectedSlice = queryAdjusted.sliceIt().fields

            // [users.name, cities.name]
            queryAdjusted.adjustSelect { select(users.name, cities.name) }
            val actualSlice = queryAdjusted.set.fields

            log.debug { "Old slice: $oldSlice" }
            log.debug { "Expected slice: $expectedSlice" }
            log.debug { "Actual slice: $actualSlice" }

            assertFalse { oldSlice.size == actualSlice.size && oldSlice.all { it in actualSlice } }
            actualSlice shouldBeEqualTo expectedSlice
            queryAdjusted.assertQueryResultValid()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `adjust query slice with empty list will throw`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val originalQuery = cities.select(cities.name)

            assertFailsWith<IllegalArgumentException> {
                originalQuery.adjustSelect { select(emptyList()) }.toList()
            }
        }
    }

    /**
     * `adjustColumnSet` 함수를 사용하여 기존 Query 에서 ColumnSet 을 변경할 수 있다.
     *
     * ```sql
     * -- Postgres
     * SELECT USERS."name", CITIES."name"
     *   FROM USERS INNER JOIN CITIES ON CITIES.CITY_ID = USERS.CITY_ID
     *  WHERE (USERS.ID = 'andrey')
     *     OR (USERS."name" = 'Sergey')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `adjust query column set`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val queryAdjusted: Query = users
                .select(users.name, cities.name)
                .where(predicate)

            val oldColumnSet = queryAdjusted.set.source
            val expectedColumnSet = users innerJoin cities

            queryAdjusted.adjustColumnSet { innerJoin(cities) }
            val actualColumnSet = queryAdjusted.set.source

            fun ColumnSet.repr(): String = QueryBuilder(false)
                .also { this.describe(TransactionManager.current(), it) }
                .toString()

            actualColumnSet.repr() shouldNotBeEqualTo oldColumnSet.repr()
            actualColumnSet.repr() shouldBeEqualTo expectedColumnSet.repr()
            queryAdjusted.assertQueryResultValid()
        }
    }

    private fun Op<Boolean>.repr(): String {
        val builder = QueryBuilder(false)
        builder.append(this)
        return builder.toString()
    }

    /**
     * `adjustWhere` 함수를 사용하여 WHERE 절을 변경할 수 있다.
     *
     * ```sql
     * SELECT users."name", cities."name"
     *   FROM users INNER JOIN cities ON cities.city_id = users.city_id
     *  WHERE (users.id = 'andrey')
     *     OR (users."name" = 'Sergey')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `adjust query where`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val queryAdjusted: Query = (users innerJoin cities)
                .select(users.name, cities.name)

            queryAdjusted.adjustWhere {
                this.shouldBeNull()  // queryAdjusted.where should be null
                predicate
            }

            // (USERS.ID = 'andrey') OR (USERS."name" = 'Sergey')
            val actualWhere = queryAdjusted.where
            log.debug { "actual where=${actualWhere?.repr()}" }

            actualWhere!!.repr() shouldBeEqualTo predicate.repr()
            queryAdjusted.assertQueryResultValid()
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT users."name", cities."name"
     *   FROM users INNER JOIN cities ON cities.city_id = users.city_id
     *  WHERE (users.id = 'andrey')
     *     OR (users."name" = 'Sergey')
     *     OR (users.id = 'andrey')
     *     OR (users."name" = 'Sergey')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `query or where`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val queryAdjusted = (users innerJoin cities)
                .select(users.name, cities.name)
                .where { predicate }

            queryAdjusted.orWhere {
                predicate
            }

            // (USERS.ID = 'andrey') OR (USERS."name" = 'Sergey') OR (USERS.ID = 'andrey') OR (USERS."name" = 'Sergey')
            val actualWhere = queryAdjusted.where
            log.debug { "actual where=${actualWhere?.repr()}" }

            actualWhere!!.repr() shouldBeEqualTo (predicate or predicate).repr()
            queryAdjusted.assertQueryResultValid()
        }
    }

    /**
     * `adjustHaving` 함수를 사용하여 HAVING 절을 변경할 수 있다.
     *
     * ```sql
     * -- Postgres
     * SELECT CITIES."name"
     *   FROM CITIES INNER JOIN USERS ON CITIES.CITY_ID = USERS.CITY_ID
     *  GROUP BY CITIES."name"
     * HAVING COUNT(USERS.ID) = MAX(CITIES.CITY_ID)
     *  ORDER BY CITIES."name" ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `adjust query having`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val predicateHaving: Op<Boolean> = Op.build {
                DMLTestData.Users.id.count().eq<Number, Long, Int>(DMLTestData.Cities.id.max())
            }

            val queryAdjusted: Query = (cities innerJoin users)
                .select(cities.name)
                .groupBy(cities.name)

            queryAdjusted.adjustHaving {
                this.shouldBeNull()
                predicateHaving
            }

            val actualHaving = queryAdjusted.having

            actualHaving!!.repr() shouldBeEqualTo predicateHaving.repr()
            val rows = queryAdjusted.orderBy(cities.name).toList()
            rows shouldHaveSize 2
            rows[0][cities.name] shouldBeEqualTo "Munich"
            rows[1][cities.name] shouldBeEqualTo "St. Petersburg"
        }
    }

    /**
     * `adjustHaving` 함수를 사용하여 HAVING 절을 변경할 수 있다.
     *
     * ```sql
     * SELECT CITIES."name"
     *   FROM CITIES INNER JOIN USERS ON CITIES.CITY_ID = USERS.CITY_ID
     *  GROUP BY CITIES."name"
     * HAVING (COUNT(USERS.ID) = MAX(CITIES.CITY_ID)) AND (COUNT(USERS.ID) = MAX(CITIES.CITY_ID))
     *  ORDER BY CITIES."name" ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `query and having`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val predicateHaving = Op.build {
                DMLTestData.Users.id.count().eq<Number, Long, Int>(DMLTestData.Cities.id.max())
            }

            val queryAdjusted = (cities innerJoin users)
                .select(cities.name)
                .groupBy(cities.name)
                .having { predicateHaving }

            queryAdjusted.andHaving {
                predicateHaving
            }

            val actualHaving = queryAdjusted.having
            actualHaving!!.repr() shouldBeEqualTo (predicateHaving and predicateHaving).repr()

            val rows = queryAdjusted.orderBy(cities.name).toList()
            rows shouldHaveSize 2
            rows[0][cities.name] shouldBeEqualTo "Munich"
            rows[1][cities.name] shouldBeEqualTo "St. Petersburg"
        }
    }

    /**
     * `adjustHaving` 함수를 사용하여 HAVING 절에 `OR` 을 추가합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT cities."name"
     *   FROM cities INNER JOIN users ON cities.city_id = users.city_id
     *  GROUP BY cities."name"
     * HAVING (COUNT(users.id) = MAX(cities.city_id)) OR (COUNT(users.id) = MAX(cities.city_id))
     *  ORDER BY cities."name" ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `query or having`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val predicateHaving = Op.build {
                DMLTestData.Users.id.count().eq<Number, Long, Int>(DMLTestData.Cities.id.max())
            }

            val queryAdjusted = (cities innerJoin users)
                .select(cities.name)
                .groupBy(cities.name)
                .having { predicateHaving }

            queryAdjusted.orHaving {
                predicateHaving
            }

            val actualHaving = queryAdjusted.having
            actualHaving!!.repr() shouldBeEqualTo (predicateHaving or predicateHaving).repr()

            val rows = queryAdjusted.orderBy(cities.name).toList()
            rows shouldHaveSize 2
            rows[0][cities.name] shouldBeEqualTo "Munich"
            rows[1][cities.name] shouldBeEqualTo "St. Petersburg"
        }
    }
}
