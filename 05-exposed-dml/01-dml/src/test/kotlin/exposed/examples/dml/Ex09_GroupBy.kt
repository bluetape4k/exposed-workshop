package exposed.examples.dml

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.currentDialectTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.toBigDecimal
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ExpressionWithColumnTypeAlias
import org.jetbrains.exposed.v1.core.GroupConcat
import org.jetbrains.exposed.v1.core.Max
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.avg
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.groupConcat
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.OracleDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLNGDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.core.vendors.VendorDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.core.vendors.h2Mode
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex09_GroupBy: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * [groupBy] 를 이용한 조회
     *
     * ```sql
     * -- Postgres
     * SELECT cities."name",
     *        COUNT(users.id),
     *        COUNT(users.id) c
     *   FROM cities INNER JOIN users ON cities.city_id = users.city_id
     *  GROUP BY cities."name"
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `groupBy example 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val cAlias: ExpressionWithColumnTypeAlias<Long> = users.id.count().alias("c")

            val rows: List<ResultRow> = cities.innerJoin(users)
                .select(
                    cities.name,
                    users.id.count(),
                    cAlias
                )
                .groupBy(cities.name)
                .toList()

            rows.forEach {
                val cityName = it[cities.name]
                val userCount = it[users.id.count()]
                val userCountAlias = it[cAlias]

                // cityName=St. Petersburg, userCount=1, userCountAlias=1
                // cityName=Munich, userCount=2, userCountAlias=2
                log.debug { "cityName=$cityName, userCount=$userCount, userCountAlias=$userCountAlias" }

                when (cityName) {
                    "Munich" -> userCount shouldBeEqualTo 2L
                    "Prague" -> userCount shouldBeEqualTo 0L
                    "St. Petersburg" -> userCount shouldBeEqualTo 1L
                    else -> error("Unknown city $cityName")
                }
                userCountAlias shouldBeEqualTo userCount
            }
        }
    }

    /**
     * [groupBy] 와 `having` 을 이용하여 조건에 맞는 데이터를 조회한다.
     *
     * ```sql
     * -- Postgres
     * SELECT cities."name",
     *        COUNT(users.id)
     *   FROM cities INNER JOIN users ON cities.city_id = users.city_id
     *  GROUP BY cities."name"
     * HAVING COUNT(users.id) = 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `groupBy example 02`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val rows = cities.innerJoin(users)
                .select(cities.name, users.id.count())
                .groupBy(cities.name)
                .having { users.id.count() eq 1 }
                .toList()

            rows shouldHaveSize 1
            rows[0][cities.name] shouldBeEqualTo "St. Petersburg"
            rows[0][users.id.count()] shouldBeEqualTo 1L
        }
    }

    /**
     * [groupBy] 와 `having` 을 이용하여 조건에 맞는 데이터를 조회한다.
     *
     * ```sql
     * -- Postgres
     * SELECT cities."name",
     *        COUNT(users.id),
     *        MAX(cities.city_id)
     *   FROM cities INNER JOIN users ON cities.city_id = users.city_id
     *  GROUP BY cities."name"
     * HAVING COUNT(users.id) = MAX(cities.city_id)
     *  ORDER BY cities."name" ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `groupBy example 03`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val maxExpr: Max<Int, Int> = cities.id.max()

            val rows: List<ResultRow> = cities.innerJoin(users)
                .select(cities.name, users.id.count(), maxExpr)
                .groupBy(cities.name)
                .having { users.id.count().eq<Number, Long, Int>(maxExpr) }
                .orderBy(cities.name)
                .toList()

            rows.forEach { row ->
                log.debug { "city name=${row[cities.name]}, maxExpr=${row[maxExpr]}" }
            }

            rows shouldHaveSize 2

            rows[0].let {
                it[cities.name] shouldBeEqualTo "Munich"
                it[users.id.count()] shouldBeEqualTo 2L
                it[maxExpr] shouldBeEqualTo 2
            }
            rows[1].let {
                it[cities.name] shouldBeEqualTo "St. Petersburg"
                it[users.id.count()] shouldBeEqualTo 1L
                it[maxExpr] shouldBeEqualTo 1
            }
        }
    }

    /**
     * [groupBy] 와 `having` 을 이용하여 조건에 맞는 데이터를 조회한다.
     *
     * ```sql
     * -- Postgres
     * SELECT cities."name",
     *        COUNT(users.id),
     *        MAX(cities.city_id)
     *   FROM cities INNER JOIN users ON cities.city_id = users.city_id
     *  GROUP BY cities."name"
     * HAVING COUNT(users.id) <= 42
     *  ORDER BY cities."name" ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `groupBy example 04`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->

            val rows = cities.innerJoin(users)
                .select(cities.name, users.id.count(), cities.id.max())
                .groupBy(cities.name)
                .having { users.id.count() lessEq 42L }
                .orderBy(cities.name)
                .toList()

            rows shouldHaveSize 2

            rows[0].let {
                it[cities.name] shouldBeEqualTo "Munich"
                it[users.id.count()] shouldBeEqualTo 2L
            }
            rows[1].let {
                it[cities.name] shouldBeEqualTo "St. Petersburg"
                it[users.id.count()] shouldBeEqualTo 1L
            }
        }
    }

    /**
     * MAX, MIN 등 집계 함수를 사용하여 데이터를 조회한다.
     *
     * ```sql
     * -- Postgres
     * SELECT MAX(cities.city_id)
     *   FROM cities;
     *
     * SELECT MAX(cities.city_id)
     *   FROM cities
     *  WHERE cities.city_id IS NULL;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `groupBy example 06`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val maxNullableId = cities.id.max()

            cities.select(maxNullableId)
                .map { it[maxNullableId] }
                .let { result ->
                    result shouldHaveSize 1
                    result.single().shouldNotBeNull() shouldBeEqualTo 3
                }

            cities.select(maxNullableId)
                .where { cities.id.isNull() }
                .map { it[maxNullableId] }
                .let { result ->
                    result shouldHaveSize 1
                    result.single().shouldBeNull()
                }
        }
    }

    /**
     * Aggregate function AVG
     *
     * ```sql
     * -- Postgres
     * SELECT AVG(cities.city_id)
     *   FROM cities;
     *
     * SELECT AVG(cities.city_id)
     *   FROM cities
     *  WHERE cities.city_id IS NULL;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `groupBy example 07`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val avgIdExpr = cities.id.avg()

            val avgId = cities.select(cities.id)
                .map { it[cities.id] }
                .average()
                .toBigDecimal()
                .setScale(2)

            cities.select(avgIdExpr)
                .map { it[avgIdExpr] }
                .let { result ->
                    result shouldHaveSize 1
                    result.single()?.toBigDecimal() shouldBeEqualTo avgId
                }

            cities.select(avgIdExpr)
                .where { cities.id.isNull() }
                .map { it[avgIdExpr] }
                .let { result ->
                    result shouldHaveSize 1
                    result.single().shouldBeNull()
                }
        }
    }

    /**
     * [GroupConcat] 을 이용하여 컬럼 값을 집계한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `group concat`(testDB: TestDB) {

        withCitiesAndUsers(testDB) { cities, users, _ ->
            fun <T: String?> GroupConcat<T>.checkExcept(
                vararg dialects: VendorDialect.DialectNameProvider,
                assert: (Map<String, String?>) -> Unit,
            ) {
                val groupConcat = this
                try {
                    val result = cities.leftJoin(users)
                        .select(cities.name, groupConcat)
                        .groupBy(cities.id, cities.name)
                        .associate { it[cities.name] to it[groupConcat] }
                    assert(result)
                } catch (e: UnsupportedByDialectException) {
                    log.warn(e) { "Unsupported by dialect: ${e.dialect}" }

                    val dialectNames = dialects.map { it.dialectName }
                    val dialect = e.dialect
                    val check = when {
                        dialect.name in dialectNames -> true
                        dialect is H2Dialect && dialect.delegatedDialectNameProvider != null ->
                            dialect.delegatedDialectNameProvider!!.dialectName in dialectNames
                        else -> false
                    }
                    check.shouldBeTrue()
                }
            }

            // NOTE: Postgres 와 SQL Server 에서는 separator 를 반드시 지정해야 한다.
            users.name.groupConcat().checkExcept(PostgreSQLDialect, PostgreSQLNGDialect, SQLServerDialect) {
                it.size shouldBeEqualTo 3
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT cities."name", STRING_AGG(users."name", ', ')
             *   FROM cities LEFT JOIN users ON cities.city_id = users.city_id
             *  GROUP BY cities.city_id, cities."name";
             * ```
             */
            users.name.groupConcat(separator = ", ").checkExcept {
                it.size shouldBeEqualTo 3
                it["St. Petersburg"] shouldBeEqualTo "Andrey"

                when (currentDialectTest) {
                    // return order is arbitrary if no ORDER BY is specified
                    is MariaDBDialect, is SQLiteDialect ->
                        listOf("Sergey, Eugene", "Eugene, Sergey") shouldContain it["Munich"]

                    is MysqlDialect, is SQLServerDialect -> it["Munich"] shouldBeEqualTo "Eugene, Sergey"
                    else -> it["Munich"] shouldBeEqualTo "Sergey, Eugene"
                }

                it["Prague"].shouldBeNull()
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT cities."name", STRING_AGG( DISTINCT users."name", ' | ')
             *   FROM cities LEFT JOIN users ON cities.city_id = users.city_id
             *  GROUP BY cities.city_id, cities."name";
             * ```
             */
            users.name.groupConcat(separator = " | ", distinct = true)
                .checkExcept(OracleDialect, SQLiteDialect, SQLServerDialect) {

                    it.size shouldBeEqualTo 3
                    it["St. Petersburg"] shouldBeEqualTo "Andrey"

                    when (currentDialectTest) {
                        is MariaDBDialect ->
                            listOf("Sergey | Eugene", "Eugene | Sergey") shouldContain it["Munich"]

                        is MysqlDialect, is PostgreSQLDialect -> it["Munich"] shouldBeEqualTo "Eugene | Sergey"
                        is H2Dialect -> {
                            if (currentDialect.h2Mode == H2Dialect.H2CompatibilityMode.SQLServer) {
                                it["Munich"] shouldBeEqualTo "Sergey | Eugene"
                            } else {
                                it["Munich"] shouldBeEqualTo "Eugene | Sergey"
                            }
                        }

                        else -> it["Munich"] shouldBeEqualTo "Sergey | Eugene"
                    }
                    it["Prague"].shouldBeNull()
                }

            /**
             * ```sql
             * -- Postgres
             * SELECT cities."name", STRING_AGG(users."name", ' | ' ORDER BY users."name" ASC)
             *   FROM cities LEFT JOIN users ON cities.city_id = users.city_id
             *  GROUP BY cities.city_id, cities."name";
             * ```
             */
            users.name.groupConcat(separator = " | ", orderBy = users.name to SortOrder.ASC).checkExcept {
                it.size shouldBeEqualTo 3
                it["St. Petersburg"] shouldBeEqualTo "Andrey"
                it["Munich"] shouldBeEqualTo "Eugene | Sergey"
                it["Prague"].shouldBeNull()
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT cities."name", STRING_AGG(users."name", ' | ' ORDER BY users."name" DESC)
             *   FROM cities LEFT JOIN users ON cities.city_id = users.city_id
             *  GROUP BY cities.city_id, cities."name";
             * ```
             */
            users.name.groupConcat(separator = " | ", orderBy = users.name to SortOrder.DESC).checkExcept {
                it.size shouldBeEqualTo 3
                it["St. Petersburg"] shouldBeEqualTo "Andrey"
                it["Munich"] shouldBeEqualTo "Sergey | Eugene"
                it["Prague"].shouldBeNull()
            }
        }
    }
}
