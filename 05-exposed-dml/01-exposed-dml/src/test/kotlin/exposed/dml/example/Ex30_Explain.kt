package exposed.dml.example

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.expectException
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.amshove.kluent.shouldStartWith
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteIgnoreWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.explain
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.intParam
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.union
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.vendors.H2Dialect
import org.jetbrains.exposed.sql.vendors.MysqlDialect
import org.jetbrains.exposed.sql.vendors.SQLiteDialect
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex30_Explain: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS COUNTRIES (
     *      ID INT AUTO_INCREMENT PRIMARY KEY,
     *      COUNTRY_CODE VARCHAR(8) NOT NULL
     * )
     * ```
     */
    private object Countries: IntIdTable("countries") {
        val code = varchar("country_code", 8)
    }

    /**
     * `EXPLAIN` 이 적용된 SQL 구문은 실제로 실행되지 않는다.
     *
     * ```
     * EXPLAIN INSERT INTO countries (country_code) VALUES ('ABC');
     *
     * [QUERY PLAN=Insert on countries  (cost=0.00..0.01 rows=0 width=0), QUERY PLAN=  ->  Result  (cost=0.00..0.01 rows=1 width=38)]
     * ```
     *
     * ```
     * EXPLAIN UPDATE countries SET country_code='DEF';
     *
     * [QUERY PLAN=Update on countries  (cost=0.00..22.30 rows=0 width=0), QUERY PLAN=  ->  Seq Scan on countries  (cost=0.00..22.30 rows=1230 width=40)]
     * ```
     *
     * ```
     * EXPLAIN DELETE FROM countries
     *
     * [QUERY PLAN=Delete on countries  (cost=0.00..22.30 rows=0 width=0), QUERY PLAN=  ->  Seq Scan on countries  (cost=0.00..22.30 rows=1230 width=6)]
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `explain with statements not executed`(testDB: TestDB) {
        withTables(testDB, Countries) {
            val originalCode = "ABC"

            explain { Countries.insert { it[code] = originalCode } }.toList().apply {
                log.debug { "EXPLAIN Insert: $this" }
            }
            Countries.selectAll().empty().shouldBeTrue()

            Countries.insert { it[code] = originalCode }
            Countries.selectAll().count() shouldBeEqualTo 1L

            // EXPLAIN UPDATE COUNTRIES SET COUNTRY_CODE = 'DEF'
            explain { Countries.update { it[code] = "DEF" } }.toList().apply {
                log.debug { "EXPLAIN Update: $this" }
            }
            Countries.selectAll().single()[Countries.code] shouldBeEqualTo originalCode
            Countries.update { it[code] = "DEF" }
            Countries.selectAll().single()[Countries.code] shouldBeEqualTo "DEF"

            // EXPLAIN DELETE FROM COUNTRIES
            explain { Countries.deleteAll() }.toList().apply {
                log.debug { "EXPLAIN Delete: $this" }
            }
            Countries.selectAll().count() shouldBeEqualTo 1L

            Countries.deleteAll() shouldBeEqualTo 1
            Countries.selectAll().empty().shouldBeTrue()
        }
    }

    /**
     * `EXPLAIN` 으로 시작하는 SQL 구문은 실행되지 않는다.
     *
     * Postgres:
     * ```sql
     * EXPLAIN UPDATE userdata SET "value"=123 FROM users WHERE users.id = userdata.user_id
     * ```
     * Explain 결과:
     * ```
     * QUERY PLAN=Update on userdata  (cost=19.45..36.40 rows=0 width=0)
     * QUERY PLAN=  ->  Hash Join  (cost=19.45..36.40 rows=550 width=16)
     * QUERY PLAN=        Hash Cond: ((userdata.user_id)::text = (users.id)::text)
     * QUERY PLAN=        ->  Seq Scan on userdata  (cost=0.00..15.50 rows=550 width=44)
     * QUERY PLAN=        ->  Hash  (cost=14.20..14.20 rows=420 width=44)
     * QUERY PLAN=              ->  Seq Scan on users  (cost=0.00..14.20 rows=420 width=44)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `explain with all valid statements not executed`(testDB: TestDB) {
        var explainCount = 0
        val cityName = "City A"

        fun Transaction.explainAndIncrement(body: Transaction.() -> Any?) =
            explain(body = body).also {
                it.toList() // as with select queries, explain is only executed when iterated over
                    .apply {
                        log.debug { "EXPLAIN:\n${this.joinToString("\n")}" }
                    }
                explainCount++
            }

        withCitiesAndUsers(testDB) { cities, users, userData ->
            val testDialect = currentDialectTest
            debug = true
            statementCount = 0

            // select statements
            explainAndIncrement {
                cities.select(cities.id).where { cities.name like "A%" }
            }
            explainAndIncrement {
                (users innerJoin cities).select(users.name, cities.name)
                    .where { (users.id.eq("andrey") or users.name.eq("sergey")) and users.cityId.eq(cities.id) }
            }
            explainAndIncrement {
                val query1 = users.selectAll().where { users.id eq "andrey" }
                val query2 = users.selectAll().where { users.id eq "sergey" }
                query1.union(query2).limit(1)
            }
            // insert statements
            explainAndIncrement { cities.insert { it[name] = cityName } }
            val subquery = userData.select(userData.userId, userData.comment, intParam(42))
            explainAndIncrement { userData.insert(subquery) }
            // insert or... statements
            if (testDialect !is H2Dialect) {
                explainAndIncrement { cities.insertIgnore { it[name] = cityName } }
                explainAndIncrement { userData.insertIgnore(subquery) }
            }
            if (testDialect is MysqlDialect || testDialect is SQLiteDialect) {
                explainAndIncrement { cities.replace { it[name] = cityName } }
            }
            explainAndIncrement {
                cities.upsert {
                    it[id] = 1
                    it[name] = cityName
                }
            }
            // update statements
            explainAndIncrement { cities.update { it[name] = cityName } }
            if (testDialect !is SQLiteDialect) {
                explainAndIncrement {
                    val join = users.innerJoin(userData)
                    join.update { it[userData.value] = 123 }
                }
            }
            // delete statements
            explainAndIncrement { cities.deleteWhere { cities.id eq 1 } }
            if (testDialect is MysqlDialect) {
                explainAndIncrement { cities.deleteIgnoreWhere { cities.id eq 1 } }
            }
            explainAndIncrement { cities.deleteAll() }

            statementCount shouldBeEqualTo explainCount
            statementStats.keys.all { it.startsWith("EXPLAIN ") }.shouldBeTrue()

            debug = false
        }
    }

    /**
     * `EXPLAIN` 명령어를 사용할 때 `ANALYZE` 옵션을 사용할 수 있다.
     *
     * **`ANALYZE` 옵션을 적용하면 모든 래핑된 구문도 실행된다.**
     *
     * 단, MySQL V8만 SELECT 구문에 `ANALYZE` 옵션을 지원한다.
     *
     * ```sql
     * EXPLAIN ANALYZE INSERT INTO COUNTRIES (COUNTRY_CODE) VALUES ('ABC')
     * ```
     * =>
     * ```
     * QUERY PLAN=Insert on countries  (cost=0.00..0.01 rows=0 width=0) (actual time=0.396..0.397 rows=0 loops=1)
     * QUERY PLAN=  ->  Result  (cost=0.00..0.01 rows=1 width=38) (actual time=0.079..0.080 rows=1 loops=1)
     * QUERY PLAN=Planning Time: 0.031 ms
     * QUERY PLAN=Execution Time: 0.412 ms
     * ```
     *
     * ```sql
     * EXPLAIN ANALYZE UPDATE COUNTRIES SET COUNTRY_CODE='DEF'
     * ```
     * =>
     * ```
     * QUERY PLAN=Update on countries  (cost=0.00..22.30 rows=0 width=0) (actual time=0.026..0.026 rows=0 loops=1)
     * QUERY PLAN=  ->  Seq Scan on countries  (cost=0.00..22.30 rows=1230 width=40) (actual time=0.005..0.005 rows=1 loops=1)
     * QUERY PLAN=Planning Time: 0.038 ms
     * QUERY PLAN=Execution Time: 0.213 ms
     * ```
     *
     * ```sql
     * EXPLAIN ANALYZE DELETE FROM COUNTRIES
     * ```
     * =>
     * ```
     * QUERY PLAN=Delete on countries  (cost=0.00..22.30 rows=0 width=0) (actual time=0.012..0.012 rows=0 loops=1)
     * QUERY PLAN=  ->  Seq Scan on countries  (cost=0.00..22.30 rows=1230 width=6) (actual time=0.006..0.006 rows=1 loops=1)
     * QUERY PLAN=Planning Time: 0.019 ms
     * QUERY PLAN=Execution Time: 0.019 ms
     * ```
     *
     * ```sql
     * EXPLAIN ANALYZE SELECT COUNTRIES.ID, COUNTRIES.COUNTRY_CODE FROM COUNTRIES
     * ```
     * =>
     * ```
     * QUERY PLAN=Seq Scan on countries  (cost=0.00..22.30 rows=1230 width=38) (actual time=0.005..0.005 rows=0 loops=1)
     * QUERY PLAN=Planning Time: 0.021 ms
     * QUERY PLAN=Execution Time: 0.009 ms
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `explain with alalyze`(testDB: TestDB) {
        withTables(testDB, Countries) {
            val originalCode = "ABC"

            // MYSQL에서는 ANALYZE 옵션을 SELECT 구문에만 사용할 수 있다.
            if (testDB !in TestDB.ALL_MYSQL) {
                // `analyze` 옵션을 적용하면 모든 래핑된 구문도 실행된다.
                // EXPLAIN ANALYZE INSERT INTO COUNTRIES (COUNTRY_CODE) VALUES ('ABC')
                explain(analyze = true) { Countries.insert { it[code] = originalCode } }.toList().apply {
                    log.debug { "EXPLAIN Insert:\n${this.joinToString("\n")}" }
                }
                Countries.selectAll().count().toInt() shouldBeEqualTo 1

                // EXPLAIN ANALYZE UPDATE COUNTRIES SET COUNTRY_CODE='DEF'
                explain(analyze = true) { Countries.update { it[code] = "DEF" } }.toList().apply {
                    log.debug { "EXPLAIN Update:\n${this.joinToString("\n")}" }
                }
                Countries.selectAll().single()[Countries.code] shouldBeEqualTo "DEF"

                // EXPLAIN ANALYZE DELETE FROM COUNTRIES
                explain(analyze = true) { Countries.deleteAll() }.toList().apply {
                    log.debug { "EXPLAIN Delete:\n${this.joinToString("\n")}" }
                }
                Countries.selectAll().empty().shouldBeTrue()
            }

            // MySQL V8 이전 버전은 EXPLAIN 명령어에 ANALYZE 옵션을 지원하지 않는다.
            val analyze = testDB != TestDB.MYSQL_V5
            // EXPLAIN ANALYZE SELECT COUNTRIES.ID, COUNTRIES.COUNTRY_CODE FROM COUNTRIES
            explain(analyze = analyze) { Countries.selectAll() }.toList().apply {
                log.debug { "EXPLAIN Select:\n${this.joinToString("\n")}" }
            }
        }
    }

    /**
     * MYSQL V8 에서는 `EXPLAIN` 명령어에 `FORMAT=JSON` 옵션을 사용할 수 있다.
     *
     * Postgres 에서는 `EXPLAIN` 명령어에 `FORMAT JSON` 옵션을 사용할 수 있다.
     *
     * ```sql
     * EXPLAIN (FORMAT JSON) SELECT countries.id FROM countries WHERE countries.country_code LIKE 'A%'
     * ```
     * =>
     * ```json
     * [
     *   {
     *     "Plan": {
     *       "Node Type": "Seq Scan",
     *       "Parallel Aware": false,
     *       "Async Capable": false,
     *       "Relation Name": "countries",
     *       "Alias": "countries",
     *       "Startup Cost": 0.00,
     *       "Total Cost": 25.38,
     *       "Plan Rows": 6,
     *       "Plan Width": 4,
     *       "Filter": "((country_code)::text ~~ 'A%'::text)"
     *     }
     *   }
     * ]
     * ```
     *
     * ANALYZE 옵션과 함께 사용할 수 있다. (구문이 실행됩니다)
     * ```
     * EXPLAIN (ANALYZE TRUE, FORMAT JSON) SELECT countries.id FROM countries WHERE countries.country_code LIKE 'A%'
     * ```
     * =>
     * ```json
     * [
     *   {
     *     "Plan": {
     *       "Node Type": "Seq Scan",
     *       "Parallel Aware": false,
     *       "Async Capable": false,
     *       "Relation Name": "countries",
     *       "Alias": "countries",
     *       "Startup Cost": 0.00,
     *       "Total Cost": 25.38,
     *       "Plan Rows": 6,
     *       "Plan Width": 4,
     *       "Actual Startup Time": 0.006,
     *       "Actual Total Time": 0.006,
     *       "Actual Rows": 0,
     *       "Actual Loops": 1,
     *       "Filter": "((country_code)::text ~~ 'A%'::text)",
     *       "Rows Removed by Filter": 0
     *     },
     *     "Planning Time": 0.095,
     *     "Triggers": [
     *     ],
     *     "Execution Time": 0.020
     *   }
     * ]
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `explain with options`(testDB: TestDB) {
        val optionsAvailableDb = TestDB.ALL_POSTGRES + TestDB.ALL_MYSQL

        Assumptions.assumeTrue { testDB in optionsAvailableDb }

        withTables(testDB, Countries) {
            val formatOption = when (testDB) {
                in TestDB.ALL_MYSQL_LIKE -> "FORMAT=JSON"
                in TestDB.ALL_POSTGRES -> "FORMAT JSON"
                else -> throw UnsupportedOperationException("Format option not provided for this dialect")
            }

            val query = Countries.select(Countries.id).where { Countries.code like "A%" }
            val result = explain(options = formatOption) { query }.single()
            val jsonString = result.toString().substringAfter("=")
            log.debug { "JSON:\n$jsonString" }

            when (testDB) {
                in TestDB.ALL_MYSQL_LIKE -> jsonString.shouldStartWith("{")
                else -> jsonString.shouldStartWith("[")
            }

            // test multiple options only
            if (testDB in TestDB.ALL_POSTGRES) {
                // EXPLAIN (VERBOSE TRUE, COSTS FALSE) SELECT countries.id FROM countries WHERE countries.country_code LIKE 'A%'
                explain(options = "VERBOSE TRUE, COSTS FALSE") { query }.toList()
            }

            // test analyze + options
            val analyze = testDB != TestDB.MYSQL_V5
            val combinedOption = if (testDB == TestDB.MYSQL_V8) "FORMAT=TREE" else formatOption
            explain(analyze, combinedOption) { query }.toList().apply {
                log.debug { "EXPLAIN ANALYZE FORMAT=TREE:\n${this.toString().substringAfter("=")}" }
            }
        }
    }

    /**
     * 1. 유효하지 않은 SQL 구문을 `EXPLAIN` 명령어로 실행하면 `IllegalStateException` 예외가 발생한다.
     *
     * 2. `EXPLAIN` 명령어는 마지막 구문만 실행된다.
     *
     * ```sql
     * EXPLAIN SELECT COUNTRIES.ID, COUNTRIES.COUNTRY_CODE FROM COUNTRIES
     * ```
     * =>
     * ```
     * QUERY PLAN=Seq Scan on countries  (cost=0.00..22.30 rows=1230 width=38)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `explain with invalid statements`(testDB: TestDB) {
        withTables(testDB, Countries) {
            expectException<IllegalStateException> {
                // EXPLAIN INSERT INTO COUNTRIES (COUNTRY_CODE) VALUES ('ABC')
                explain { Countries.insertAndGetId { it[code] = "ABC" } }
            }
            expectException<IllegalStateException> {
                explain {
                    Countries.selectAll()
                    "Last line in lambda should be expected return value - statement"
                }
            }

            debug = true
            statementCount = 0

            // explain 는 마지막 구문만 실행된다.
            explain {
                // EXPLAIN DELETE FROM COUNTRIES -> 실행되지 않습니다.
                Countries.deleteAll()
                // EXPLAIN SELECT COUNTRIES.ID, COUNTRIES.COUNTRY_CODE FROM COUNTRIES -> 실행됩니다.
                Countries.selectAll()
            }.toList().apply {
                // 마지막 구문인 select 구문만 실행된다.
                log.debug { "EXPLAIN Select:\n${this.joinToString("\n")}" }
            }

            statementCount shouldBeEqualTo 1
            val executed = statementStats.keys.single()

            executed shouldStartWith "EXPLAIN "
            executed shouldContain "SELECT "            // 마지막 구문인 SELECT 구문은 실행된다.
            executed shouldNotContain "DELETE "        // DELETE 구문은 실행되지 않는다.
            debug = false
        }
    }
}
