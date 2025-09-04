package exposed.examples.dml

import exposed.shared.dml.DMLTestData.toCityNameList
import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.expectException
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.Case
import org.jetbrains.exposed.v1.core.Coalesce
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.isDistinctFrom
import org.jetbrains.exposed.v1.core.isNotDistinctFrom
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.isNullOrEmpty
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * 조건을 표현하는 다양한 [Op] 에 대한 예제 모음
 */
class Ex23_Conditions: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * [Op.TRUE], [Op.FALSE] 를 사용하여 TRUE, FALSE 조건을 표현한다.
     *
     * ```sql
     * SELECT COUNT(*) FROM CITIES WHERE FALSE
     * SELECT COUNT(*) FROM CITIES WHERE TRUE
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `TRUE and FALSE Ops`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val allCities = cities.selectAll().toCityNameList()

            cities.selectAll().where { Op.FALSE }.count() shouldBeEqualTo 0L
            cities.selectAll().where { Op.TRUE }.count() shouldBeEqualTo allCities.size.toLong()
        }
    }

    /**
     * `isNotDistinctFrom`, `isDistinctFrom` 을 이용하여 null 값도 비교할 수 있도록 한다.
     *
     * * `isNotDistinctFrom` 은 `null == null` 을 `true` 로 처리한다.
     * * `isDistinctFrom` 은 `null != null` 을 `false` 로 처리한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `null safe equality Ops`(testDB: TestDB) {
        val table = object: IntIdTable("foo") {
            val number1 = integer("number1").nullable()
            val number2 = integer("number2").nullable()
        }

        withTables(testDB, table) {
            val sameNumberId = table.insertAndGetId {
                it[number1] = 0
                it[number2] = 0
            }
            val differentNumberId = table.insertAndGetId {
                it[number1] = 0
                it[number2] = 1
            }
            val oneNullId = table.insertAndGetId {
                it[number1] = 0
                it[number2] = null
            }
            val bothNullId = table.insertAndGetId {
                it[number1] = null
                it[number2] = null
            }

            // null == null returns null
            table.selectAll()
                .where { table.number1 eq table.number2 }
                .map { it[table.id] } shouldBeEqualTo listOf(sameNumberId)

            /**
             * null == null returns true
             *
             * ```sql
             * SELECT foo.id, foo.number1, foo.number2
             *   FROM foo
             *  WHERE foo.number1 IS NOT DISTINCT FROM foo.number2
             * ```
             */
            table.selectAll()
                .where { table.number1 isNotDistinctFrom table.number2 }
                .map { it[table.id] } shouldBeEqualTo listOf(sameNumberId, bothNullId)

            // null != null return null
            table.selectAll()
                .where { table.number1 neq table.number2 }
                .map { it[table.id] } shouldBeEqualTo listOf(differentNumberId)

            /**
             * null != null return false
             *
             * ```sql
             * SELECT foo.id, foo.number1, foo.number2
             *   FROM foo
             *  WHERE foo.number1 IS DISTINCT FROM foo.number2
             * ```
             */
            table.selectAll()
                .where { table.number1 isDistinctFrom table.number2 }
                .map { it[table.id] } shouldBeEqualTo listOf(differentNumberId, oneNullId)

            /**
             * (number1 is not null) != (number2 is null) returns true when both are null or neither is null
             *
             * ```sql
             * SELECT foo.id, foo.number1, foo.number2
             *   FROM foo
             *  WHERE (foo.number1 IS NOT NULL) IS DISTINCT FROM (foo.number2 IS NULL)
             * ```
             */
            table.selectAll()
                .where { table.number1.isNotNull() isDistinctFrom table.number2.isNull() }
                .map { it[table.id] } shouldBeEqualTo listOf(sameNumberId, differentNumberId, bothNullId)

            /**
             * (number1 is not null) == (number2 is null) returns true when only 1 is null
             *
             * ```sql
             * SELECT foo.id, foo.number1, foo.number2
             *   FROM foo
             *  WHERE (foo.number1 IS NOT NULL) IS NOT DISTINCT FROM (foo.number2 IS NULL)
             * ```
             */
            table.selectAll()
                .where { table.number1.isNotNull() isNotDistinctFrom table.number2.isNull() }
                .map { it[table.id] } shouldBeEqualTo listOf(oneNullId)
        }
    }

    /**
     * `EntityID` 수형을 비교 연산에 사용하는 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `comparison operations with EntityID columns`(testDB: TestDB) {
        val longTable = object: LongIdTable("long_table") {
            val amount = long("amount")
        }

        fun selectIdWhere(condition: () -> Op<Boolean>): List<Long> {
            val query = longTable.select(longTable.id).where(condition())
            return query.map { it[longTable.id].value }
        }

        withTables(testDB, longTable) {
            val id1 = longTable.insertAndGetId {
                it[id] = 1
                it[amount] = 9999
            }.value
            val id2 = longTable.insertAndGetId {
                it[id] = 2
                it[amount] = 2
            }.value
            val id3 = longTable.insertAndGetId {
                it[id] = 3
                it[amount] = 1
            }.value


            selectIdWhere { longTable.id eq longTable.amount } shouldBeEqualTo listOf(id2)
            selectIdWhere { longTable.id neq longTable.amount } shouldBeEqualTo listOf(id1, id3)

            selectIdWhere { longTable.id less longTable.amount } shouldBeEqualTo listOf(id1)
            selectIdWhere { longTable.id less 2L } shouldBeEqualTo listOf(id1)

            selectIdWhere { longTable.id lessEq longTable.amount } shouldBeEqualTo listOf(id1, id2)
            selectIdWhere { longTable.id lessEq 2L } shouldBeEqualTo listOf(id1, id2)

            selectIdWhere { longTable.id greater longTable.amount } shouldBeEqualTo listOf(id3)
            selectIdWhere { longTable.id greater 2L } shouldBeEqualTo listOf(id3)

            selectIdWhere { longTable.id greaterEq longTable.amount } shouldBeEqualTo listOf(id2, id3)
            selectIdWhere { longTable.id greaterEq 2L } shouldBeEqualTo listOf(id2, id3)

            selectIdWhere { longTable.id.between(2, 3) } shouldBeEqualTo listOf(id2, id3)

            selectIdWhere { longTable.id isNotDistinctFrom longTable.amount } shouldBeEqualTo listOf(id2)
            selectIdWhere { longTable.id isNotDistinctFrom 2 } shouldBeEqualTo listOf(id2)

            selectIdWhere { longTable.id isDistinctFrom longTable.amount } shouldBeEqualTo listOf(id1, id3)
            selectIdWhere { longTable.id isDistinctFrom 2 } shouldBeEqualTo listOf(id1, id3)

            // symmetric operators (EntityID value on right) should not show a warning either
            selectIdWhere { longTable.amount eq longTable.id } shouldBeEqualTo listOf(id2)
            selectIdWhere { longTable.amount neq longTable.id } shouldBeEqualTo listOf(id1, id3)
            selectIdWhere { longTable.amount less longTable.id } shouldBeEqualTo listOf(id3)
            selectIdWhere { longTable.amount lessEq longTable.id } shouldBeEqualTo listOf(id2, id3)
            selectIdWhere { longTable.amount greater longTable.id } shouldBeEqualTo listOf(id1)
            selectIdWhere { longTable.amount greaterEq longTable.id } shouldBeEqualTo listOf(id1, id2)
            selectIdWhere { longTable.amount isNotDistinctFrom longTable.id } shouldBeEqualTo listOf(id2)
            selectIdWhere { longTable.amount isDistinctFrom longTable.id } shouldBeEqualTo listOf(id1, id3)

            selectIdWhere { longTable.amount.between(1L, 2L) } shouldBeEqualTo listOf(id2, id3)
        }
    }

    /**
     * 중복된 컬럼은 한 번만 사용하도록 수정된다.
     *
     * ```sql
     * SELECT CITIES."name",
     *        CITIES.CITY_ID
     *   FROM CITIES
     *  WHERE CITIES."name" = 'Munich'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `same column used in slice multiple times`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val row = cities
                .select(cities.name, cities.name, cities.id)
                .where { cities.name eq "Munich" }
                .toList()
                .single()

            row[cities.id] shouldBeEqualTo 2
            row[cities.name] shouldBeEqualTo "Munich"
        }
    }

    /**
     * SELECT 절에 아무런 컬럼이 없다면 [IllegalArgumentException] 예외가 발생한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `throw when slice with empty list`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            expectException<IllegalArgumentException> {
                cities.select(emptyList()).toList()
            }
        }
    }

    /**
     * Nullable 컬럼과의 비교
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `compare to nullable column`(testDB: TestDB) {
        val table = object: IntIdTable("foo") {
            val c1 = integer("c1")
            val c2 = integer("c2").nullable()
        }

        withTables(testDB, table) {
            table.insertAndGetId { it[c1] = 0; it[c2] = 0 }
            table.insertAndGetId { it[c1] = 1; it[c2] = 2 }
            table.insertAndGetId { it[c1] = 2; it[c2] = 1 }

            table.selectAll()
                .where { table.c1 less table.c2 }
                .map { it[table.c1] } shouldBeEqualTo listOf(1)

            table.selectAll()
                .where { table.c1 lessEq table.c2 }
                .orderBy(table.c1)
                .map { it[table.c1] } shouldBeEqualTo listOf(0, 1)

            table.selectAll()
                .where { table.c1 greater table.c2 }
                .map { it[table.c1] } shouldBeEqualTo listOf(2)

            table.selectAll()
                .where { table.c1 greaterEq table.c2 }
                .orderBy(table.c1)
                .map { it[table.c1] } shouldBeEqualTo listOf(0, 2)


            table.selectAll()
                .where { table.c2 less table.c1 }
                .map { it[table.c1] } shouldBeEqualTo listOf(2)

            table.selectAll()
                .where { table.c2 lessEq table.c1 }
                .orderBy(table.c1)
                .map { it[table.c1] } shouldBeEqualTo listOf(0, 2)

            table.selectAll()
                .where { table.c2 greater table.c1 }
                .map { it[table.c1] } shouldBeEqualTo listOf(1)

            table.selectAll()
                .where { table.c2 greaterEq table.c1 }
                .orderBy(table.c1)
                .map { it[table.c1] } shouldBeEqualTo listOf(0, 1)
        }
    }

    /**
     * FK에 해당하는 컬럼 값은 `Op.nullOp()` 을 사용하여 NULL 로 지정할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `nullOp update and select`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val allUserCount = users.selectAll().count()

            /**
             * ```sql
             * UPDATE USERS SET CITY_ID=NULL
             * ```
             */
            users.update {
                it[users.cityId] = Op.nullOp()
            }

            // SELECT COUNT(*) FROM USERS WHERE USERS.CITY_ID IS NULL
            val nullUsersOp = users.selectAll().where { users.cityId eq Op.nullOp() }.count()
            nullUsersOp shouldBeEqualTo allUserCount

            /**
             * ```sql
             * UPDATE USERS SET CITY_ID=NULL
             * ```
             */
            users.update {
                it[users.cityId] = null
            }

            val nullUsers = users.selectAll().where { users.cityId.isNull() }.count()
            nullUsers shouldBeEqualTo allUserCount

            val nullUsersEq = users.selectAll().where { users.cityId eq null }.count()
            nullUsersEq shouldBeEqualTo allUserCount
        }
    }

    /**
     * NON NULL 컬럼에 `Op.nullOp()` 을 사용하면 [ExposedSQLException] 예외가 발생한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `nullOp update fails when apply to non-null column`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            expectException<ExposedSQLException> {
                users.update {
                    it[users.name] = Op.nullOp()
                }
            }
        }
    }

    /**
     * `Case` 구문에 `Op.nullOp()` 을 사용하여 NULL 값을 반환할 수 있다.
     *
     * ```sql
     * SELECT CITIES.CITY_ID,
     *        CITIES."name",
     *        CASE
     *          WHEN CITIES.CITY_ID = 1 THEN NULL
     *          ELSE CITIES."name"
     *        END
     *   FROM CITIES
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `nullOp in case`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val caseCondition = Case()
                .When(cities.id eq 1, Op.nullOp<String>())
                .Else(cities.name)

            var nullBranchWasExecuted = false

            cities.select(cities.id, cities.name, caseCondition).forEach {
                val result = it[caseCondition]
                if (it[cities.id] == 1) {
                    nullBranchWasExecuted = true
                    result.shouldBeNull()
                } else {
                    result shouldBeEqualTo it[cities.name]
                }
            }
            nullBranchWasExecuted.shouldBeTrue()
        }
    }

    /**
     * `Case` 구문에 `Op.nullOp()` 을 사용하여 NULL 값을 반환할 수 있다.
     *
     * ```sql
     * SELECT CITIES.CITY_ID,
     *        COALESCE(CASE
     *                  WHEN CITIES.CITY_ID = 1 THEN 'ORIGINAL'
     *                  ELSE NULL
     *                 END,
     *                 'COPY'
     *        )
     *   FROM CITIES
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `CaseWhenElse as argument`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val original = "ORIGINAL"
            val copy = "COPY"
            val condition = cities.id eq 1

            val caseCondition1 = Case()
                .When(condition, stringLiteral(original))
                .Else(Op.nullOp())

            // Case().When().Else() invokes CaseWhenElse() so the 2 formats should be interchangeable as arguments
            val caseCondition2 = Case()
                .When(condition, stringLiteral(original))
                .Else(Op.nullOp())
            val function1 = Coalesce(caseCondition1, stringLiteral(copy))
            val function2 = Coalesce(caseCondition2, stringLiteral(copy))

            // confirm both formats produce identical SQL
            val query1 = cities.select(cities.id, function1).prepareSQL(this, prepared = false)
            val query2 = cities.select(cities.id, function2).prepareSQL(this, prepared = false)
            log.debug { "Query: $query1" }
            query1 shouldBeEqualTo query2

            val results1 = cities.select(cities.id, function1).toList()

            cities.select(cities.id, function2).forEachIndexed { i, row ->
                val currentId = row[cities.id]
                val functionResult = row[function2]
                log.debug { "currentId: $currentId, functionResult: $functionResult" }

                functionResult shouldBeEqualTo (if (currentId == 1) original else copy)
                results1[i][cities.id] shouldBeEqualTo currentId
                results1[i][function1] shouldBeEqualTo functionResult
            }
        }
    }

    /**
     * Nested CaseWhenElse syntax
     *
     * ```sql
     * SELECT CITIES."name",
     *        CASE
     *            WHEN CITIES."name" LIKE 'M%' THEN 0
     *            WHEN CITIES."name" LIKE 'St. %' THEN
     *                CASE
     *                    WHEN CITIES.CITY_ID = 1 THEN 1
     *                    ELSE -1
     *                END
     *            WHEN CITIES."name" LIKE 'P%' THEN 2
     *            ELSE -1
     *        END
     *  FROM CITIES
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `chained and nested CaseWhenElse syntax`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val nestedCondition = Case()
                // .When(cities.id eq 1, intLiteral(1)) 처럼 Op.build {} 를 안 써도 된다.
                .When(cities.id eq 1, intLiteral(1))
                .Else(intLiteral(-1))

            val chainedCondition = Case()
                .When(cities.name like "M%", intLiteral(0))
                .When(cities.name like "St. %", nestedCondition)
                .When(cities.name like "P%", intLiteral(2))
                .Else(intLiteral(-1))

            val results = cities.select(cities.name, chainedCondition)

            results.forEach {
                val cityName = it[cities.name]
                val expectedNumber = when {
                    cityName.startsWith("M") -> 0
                    cityName.startsWith("St. ") -> 1
                    cityName.startsWith("P") -> 2
                    else -> -1
                }
                it[chainedCondition] shouldBeEqualTo expectedNumber
            }
        }
    }

    /**
     * 비교 연산을 수행한 게산된 컬럼을 alias 로 지정하여 반환한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select aliased comparison result`(testDB: TestDB) {
        val table = object: IntIdTable("foo") {
            val c1 = integer("c1")
            val c2 = integer("c2").nullable()
        }

        withTables(testDB, table) {
            table.insert { it[c1] = 0; it[c2] = 0 }
            table.insert { it[c1] = 1; it[c2] = 2 }
            table.insert { it[c1] = 2; it[c2] = 1 }

            /**
             * ```sql
             * SELECT FOO.C1 < FOO.C2 c1_lt_c2
             *   FROM FOO
             *  ORDER BY FOO.C1 ASC
             * ```
             */
            val c1_lt_c2 = table.c1.less(table.c2).alias("c1_lt_c2")
            table.select(c1_lt_c2)
                .orderBy(table.c1)
                .map { it[c1_lt_c2] } shouldBeEqualTo listOf(false, true, false)

            /**
             * ```sql
             * SELECT FOO.C1 <= FOO.C2 c1_lte_c2
             *   FROM FOO
             *  ORDER BY FOO.C1 ASC
             *  ```
             */
            val c1_lte_c2 = table.c1.lessEq(table.c2).alias("c1_lte_c2")
            table.select(c1_lte_c2)
                .orderBy(table.c1)
                .map { it[c1_lte_c2] } shouldBeEqualTo listOf(true, true, false)

            /**
             * ```sql
             * SELECT FOO.C1 > FOO.C2 c1_gt_c2
             *   FROM FOO
             *  ORDER BY FOO.C1 ASC
             * ```
             */
            val c1_gt_c2 = table.c1.greater(table.c2).alias("c1_gt_c2")
            table.select(c1_gt_c2)
                .orderBy(table.c1)
                .map { it[c1_gt_c2] } shouldBeEqualTo listOf(false, false, true)

            /**
             * ```sql
             * SELECT FOO.C1 >= FOO.C2 c1_gte_c2
             *   FROM FOO
             *  ORDER BY FOO.C1 ASC
             * ```
             */
            val c1_gte_c2 = table.c1.greaterEq(table.c2).alias("c1_gte_c2")
            table.select(c1_gte_c2)
                .orderBy(table.c1)
                .map { it[c1_gte_c2] } shouldBeEqualTo listOf(true, false, true)
        }
    }

    /**
     * `isNull`, `isNotNull`, `isNullOrEmpty` 함수를 사용하여 NULL 또는 빈 문자열을 비교한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `null or empty`(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      id SERIAL PRIMARY KEY,
         *      "name" TEXT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("tester") {
            val name = text("name").nullable()
        }

        withTables(testDB, tester) {

            tester.insert { it[name] = null }
            tester.insert { it[name] = "" }
            tester.insert { it[name] = "a" }

            entityCache.clear()

            /**
             * ```sql
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE tester."name" IS NULL
             * ```
             */
            tester.selectAll()
                .where { tester.name.isNull() }
                .count() shouldBeEqualTo 1L

            /**
             * ```sql
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE (tester."name" IS NULL) OR (CHAR_LENGTH(tester."name") = 0)
             * ```
             */
            tester.selectAll()
                .where { tester.name.isNullOrEmpty() }
                .count() shouldBeEqualTo 2L

            /**
             * ```sql
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE tester."name" = ''
             * ```
             */
            tester.selectAll()
                .where { tester.name eq "" }
                .count() shouldBeEqualTo 1L

            /**
             * ```sql
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE tester."name" IS NOT NULL
             * ```
             */
            tester.selectAll()
                .where { tester.name neq null }
                .count() shouldBeEqualTo 2L

            /**
             * ```sql
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE tester."name" IS NOT NULL
             */
            tester.selectAll()
                .where { tester.name.isNotNull() }
                .count() shouldBeEqualTo 2L

            /**
             * ```sql
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE tester."name" <> ''
             */
            tester.selectAll()
                .where { tester.name neq "" }
                .count() shouldBeEqualTo 1L
        }
    }
}
