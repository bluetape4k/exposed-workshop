package exposed.dml.example

import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryAlias
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times
import org.jetbrains.exposed.sql.Table.Dual
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.mergeFrom
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.stringLiteral
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * `MERGE INTO` 구문은 데이터베이스에서 조건에 따라 데이터를 삽입, 갱신, 삭제하는 작업을 한 번에 수행할 수 있게 해주는 강력한 기능입니다
 *
 * 참고: [SQL MERGE INTO 설명](https://www.perplexity.ai/search/sql-merge-into-gumune-daehae-s-y_xKDfwFR8ewN6qIY9jqJw)
 */
class Ex14_MergeSelect: Ex14_MergeBase() {

    companion object: KLogging()

    private val sourceQuery: QueryAlias = Source.selectAll().alias("sub")

    private fun SqlExpressionBuilder.defaultOnCondition(): Op<Boolean> =
        Dest.key eq sourceQuery[Source.key]

    /**
     * [mergeFrom] (`MERGE INFO`) from a select query
     *
     * 대상 테이블에 없는 소스 테이블의 데이터를 대상 테이블에 추가합니다
     *
     * ```sql
     * -- Postgres
     * MERGE INTO dest
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source"
     *       ) as sub ON dest."key" = sub."key"
     *  WHEN NOT MATCHED THEN
     *      INSERT ("at", "key", "value", optional_value)
     *      VALUES ('2000-01-01T00:00:00', sub."key", (sub."value" * 2), CONCAT('optional::', sub."key"))
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mergeFrom with whenNotMatchedInsert`(testDB: TestDB) {
        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            dest.mergeFrom(
                sourceQuery,
                on = { defaultOnCondition() }
            ) {
                // dest 테이블에 없는 source 데이터를 dest 테이블에 추가
                whenNotMatchedInsert {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = sourceQuery[source.value] * 2
                    it[dest.optional] = stringLiteral("optional::") + sourceQuery[source.key]
                }
            }

            val destRow = dest.getByKey("only-in-source-1")
            destRow[dest.value] shouldBeEqualTo 2
            destRow[dest.optional] shouldBeEqualTo "optional::only-in-source-1"
            destRow[dest.at] shouldBeEqualTo TEST_DEFAULT_DATE_TIME
        }
    }

    /**
     * Merge into ([mergeFrom]) from a select query with alias
     *
     * `whenNotMatchedInsert`를 이용하여 대상 테이블에 없는 소스 테이블의 데이터를 대상 테이블에 추가합니다
     *
     * ```sql
     * -- Postgres
     * MERGE INTO dest dest_alias
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source"
     *       ) as sub ON sub."key" = dest_alias."key"
     *  WHEN NOT MATCHED THEN
     *      INSERT ("at", "key", "value")
     *      VALUES ('2000-01-01T00:00:00', sub."key", (sub."value" * 2))
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mergeFrom with whenNotMatchedInsert by alias`(testDB: TestDB) {
        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            val destAlias = dest.alias("dest_alias")

            destAlias.mergeFrom(
                sourceQuery,
                on = { sourceQuery[source.key] eq destAlias[dest.key] }
            ) {
                whenNotMatchedInsert {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = sourceQuery[source.value] * 2
                }
            }

            val destRow = dest.getByKey("only-in-source-1")
            destRow[dest.value] shouldBeEqualTo 2
        }
    }

    /**
     * [mergeFrom] with `whenMatchedUpdate`
     *
     * `whenMatchedUpdate` 를 이용하여 대상 테이블과 소스 테이블의 데이터가 일치하는 경우 대상 테이블의 데이터를 갱신합니다
     *
     * Postgres:
     * ```sql
     * MERGE INTO dest
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source"
     *       ) as sub ON dest."key" = sub."key"
     *  WHEN MATCHED THEN
     *      UPDATE SET "value"=((sub."value" + dest."value") * 2),
     *                 optional_value=CONCAT(CONCAT(sub."key", '::'), dest."key")
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mergeFrom with whenMatchedUpdate`(testDB: TestDB) {
        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            dest.mergeFrom(
                sourceQuery,
                on = { defaultOnCondition() }
            ) {
                whenMatchedUpdate {
                    it[dest.value] = (sourceQuery[source.value] + dest.value) * 2
                    it[dest.optional] = sourceQuery[source.key] + stringLiteral("::") + dest.key
                }
            }

            val destRow = dest.getByKey("in-source-and-dest-1")
            destRow[dest.value] shouldBeEqualTo 22
            destRow[dest.optional] shouldBeEqualTo "in-source-and-dest-1::in-source-and-dest-1"
            destRow[dest.at] shouldBeEqualTo TEST_DEFAULT_DATE_TIME
        }
    }

    /**
     * [mergeFrom] with `whenMatchedUpdate`
     *
     * `whenMatchedUpdate` 를 이용하여 대상 테이블과 소스 테이블의 데이터가 일치하는 경우 대상 테이블의 데이터를 갱신합니다
     *
     * ```sql
     * -- Postgres
     * MERGE INTO dest dest_alias
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source"
     *       ) as sub ON sub."key" = dest_alias."key"
     *  WHEN MATCHED THEN
     *      UPDATE SET "value"=((sub."value" + dest_alias."value") * 2),
     *                 optional_value=CONCAT(CONCAT(sub."key", '::'), dest_alias."key")
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mergeFrom with whenMatchedUpdate by alias`(testDB: TestDB) {
        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            val destAlias = dest.alias("dest_alias")

            destAlias.mergeFrom(
                sourceQuery,
                on = { sourceQuery[source.key] eq destAlias[dest.key] }
            ) {
                whenMatchedUpdate {
                    it[dest.value] = (sourceQuery[source.value] + destAlias[dest.value]) * 2
                    it[dest.optional] = sourceQuery[source.key] + stringLiteral("::") + destAlias[dest.key]
                }
            }

            val destRow = dest.getByKey("in-source-and-dest-1")
            destRow[dest.value] shouldBeEqualTo 22
            destRow[dest.optional] shouldBeEqualTo "in-source-and-dest-1::in-source-and-dest-1"
            destRow[dest.at] shouldBeEqualTo TEST_DEFAULT_DATE_TIME
        }
    }

    /**
     * [mergeFrom] with `whenMatchedDelete`
     *
     * `whenMatchedDelete` 를 이용하여 대상 테이블과 소스 테이블의 데이터가 일치하는 경우 대상 테이블의 데이터를 삭제합니다
     *
     * ```sql
     * -- Postgres
     * MERGE INTO dest
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source"
     *       ) as sub ON dest."key" = sub."key"
     *  WHEN MATCHED THEN
     *      DELETE
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mergeFrom with whenMatchedDelete`(testDB: TestDB) {
        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            dest.mergeFrom(
                sourceQuery,
                on = { defaultOnCondition() }
            ) {
                whenMatchedDelete()
            }

            dest.getByKeyOrNull("in-source-and-dest-1").shouldBeNull()
            dest.getByKeyOrNull("in-source-and-dest-2").shouldBeNull()
            dest.getByKeyOrNull("in-source-and-dest-3").shouldBeNull()
            dest.getByKeyOrNull("in-source-and-dest-4").shouldBeNull()
        }
    }

    /**
     * [mergeFrom] with `whenNotMatchedInsert` and `whenMatchedUpdate`
     *
     * `whenNotMatchedInsert` 를 이용하여 대상 테이블에 없는 소스 테이블의 데이터를 추가하고,
     * `whenMatchedUpdate` 를 이용하여 대상 테이블과 소스 테이블의 데이터가 일치하는 경우 대상 테이블의 데이터를 갱신합니다.
     *
     * ```sql
     * -- Postgres
     * MERGE INTO dest
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source"
     *       ) as sub ON dest."key" = sub."key"
     *  WHEN NOT MATCHED AND (sub."value" > 2) THEN
     *      INSERT ("at", "key", "value")
     *      VALUES ('2000-01-01T00:00:00', sub."key", sub."value")
     *  WHEN MATCHED AND (dest."value" > 20) THEN
     *      UPDATE SET "value"=(sub."value" + dest."value")
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mergeFrom with whenNotMatchedInsert and whenMatchedUpdate`(testDB: TestDB) {
        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            dest.mergeFrom(
                sourceQuery,
                on = { defaultOnCondition() }
            ) {
                whenNotMatchedInsert(and = (sourceQuery[source.value] greater 2)) {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = sourceQuery[source.value]
                }

                whenMatchedUpdate(and = (dest.value greater 20)) {
                    it[dest.value] = sourceQuery[source.value] + dest.value
                }
            }

            dest.getByKeyOrNull("only-in-source-1").shouldBeNull()
            dest.getByKeyOrNull("only-in-source-2").shouldBeNull()
            dest.getByKeyOrNull("only-in-source-3").shouldNotBeNull()
            dest.getByKeyOrNull("only-in-source-4").shouldNotBeNull()

            dest.getByKey("in-source-and-dest-1")[dest.value] shouldBeEqualTo 10
            dest.getByKey("in-source-and-dest-2")[dest.value] shouldBeEqualTo 20
            dest.getByKey("in-source-and-dest-3")[dest.value] shouldBeEqualTo 33
            dest.getByKey("in-source-and-dest-4")[dest.value] shouldBeEqualTo 44
        }
    }

    /**
     * MergeFrom with whenMatchedDelete and condition
     *
     * ```sql
     * -- Postgres
     * MERGE INTO dest
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source"
     *        ) as sub ON dest."key" = sub."key"
     *  WHEN MATCHED AND ((sub."value" > 2) AND (dest."value" > 20)) THEN
     *      DELETE
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mergeFrom with whenMatchedDelete and condition`(testDB: TestDB) {
        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            dest.mergeFrom(
                sourceQuery,
                on = { defaultOnCondition() }
            ) {
                whenMatchedDelete(and = (sourceQuery[source.value] greater 2) and (dest.value greater 20))
            }

            dest.getByKeyOrNull("in-source-and-dest-1").shouldNotBeNull()
            dest.getByKeyOrNull("in-source-and-dest-2").shouldNotBeNull()
            dest.getByKeyOrNull("in-source-and-dest-3").shouldBeNull()
            dest.getByKeyOrNull("in-source-and-dest-4").shouldBeNull()
        }
    }

    /**
     * [mergeFrom] 과 다양한 조건 작업들 ...
     *
     * Postgres:
     * ```sql
     * MERGE INTO dest
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source"
     *        ) as sub ON dest."key" = sub."key"
     *  WHEN NOT MATCHED AND (sub."value" = 1) THEN
     *      INSERT ("at", "key", "value", optional_value)
     *      VALUES ('2000-01-01T00:00:00', sub."key", sub."value", 'one')
     *  WHEN NOT MATCHED AND (sub."value" = 2) THEN
     *      INSERT ("at", "key", "value", optional_value)
     *      VALUES ('2000-01-01T00:00:00', sub."key", sub."value", 'two')
     *  WHEN NOT MATCHED THEN
     *      INSERT ("at", "key", "value", optional_value)
     *      VALUES ('2000-01-01T00:00:00', sub."key", sub."value", 'three-and-more')
     *  WHEN MATCHED AND (sub."value" = 1) THEN
     *      DELETE
     *  WHEN MATCHED AND (sub."value" = 1) THEN
     *      UPDATE SET "key"=sub."key",
     *                 "value"=((dest."value" + sub."value") * 10)
     *  WHEN MATCHED AND (sub."value" = 2) THEN
     *      UPDATE SET "key"=sub."key",
     *                 "value"=((dest."value" + sub."value") * 100)
     *  WHEN MATCHED AND (sub."value" = 3) THEN
     *      DELETE
     *  WHEN MATCHED THEN
     *      UPDATE SET "key"=sub."key",
     *                 "value"=1000
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mergeFrom with multiple clauses`(testDB: TestDB) {
        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            dest.mergeFrom(sourceQuery, on = { defaultOnCondition() }) {
                whenNotMatchedInsert(and = (sourceQuery[source.value] eq 1)) {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = sourceQuery[source.value]
                    it[dest.optional] = "one"
                }
                whenNotMatchedInsert(and = (sourceQuery[source.value] eq 2)) {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = sourceQuery[source.value]
                    it[dest.optional] = "two"
                }
                whenNotMatchedInsert {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = sourceQuery[source.value]
                    it[dest.optional] = "three-and-more"
                }

                whenMatchedDelete(and = (sourceQuery[source.value] eq 1))
                whenMatchedUpdate(and = (sourceQuery[source.value] eq 1)) {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = (dest.value + sourceQuery[source.value]) * 10
                }
                whenMatchedUpdate(and = (sourceQuery[source.value] eq 2)) {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = (dest.value + sourceQuery[source.value]) * 100
                }
                whenMatchedDelete(and = (sourceQuery[source.value] eq 3))

                whenMatchedUpdate {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = 1000
                }
            }

            dest.getByKey("only-in-source-1")[dest.optional] shouldBeEqualTo "one"
            dest.getByKey("only-in-source-2")[dest.optional] shouldBeEqualTo "two"
            dest.getByKey("only-in-source-3")[dest.optional] shouldBeEqualTo "three-and-more"
            dest.getByKey("only-in-source-4")[dest.optional] shouldBeEqualTo "three-and-more"


            dest.getByKeyOrNull("in-source-and-dest-1").shouldBeNull()
            dest.getByKey("in-source-and-dest-2")[dest.value] shouldBeEqualTo 2200
            dest.getByKeyOrNull("in-source-and-dest-3").shouldBeNull()
            dest.getByKey("in-source-and-dest-4")[dest.value] shouldBeEqualTo 1000
        }
    }

    /**
     * Subquery 를 Source로 사용하는 [mergeFrom] 예제 (PostgreSQL 전용)
     *
     * Postgres:
     * ```sql
     * MERGE INTO dest
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source" ) as sub ON dest."key" = sub."key"
     *  WHEN NOT MATCHED AND (sub."value" > 1) THEN
     *      DO NOTHING
     *  WHEN NOT MATCHED THEN
     *      INSERT ("at", "key", "value")
     *      VALUES ('2000-01-01T00:00:00', sub."key", (sub."value" + 100))
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `do nothing in postgres`(testDB: TestDB) {
        Assumptions.assumeTrue(testDB in TestDB.ALL_POSTGRES)

        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            dest.mergeFrom(sourceQuery, on = { defaultOnCondition() }) {
                whenNotMatchedDoNothing(and = sourceQuery[source.value] greater 1)
                whenNotMatchedInsert {
                    it[dest.key] = sourceQuery[source.key]
                    it[dest.value] = sourceQuery[source.value] + 100
                }
            }

            dest.selectAll()
                .where { dest.key eq "only-in-source-1" }
                .first()[dest.value] shouldBeEqualTo 101

            dest.selectAll()
                .where { dest.key inList listOf("only-in-source-2", "only-in-source-3") }
                .firstOrNull().shouldBeNull()
        }
    }

    /**
     * MergeFrom with const condition
     *
     * Postgres:
     * ```sql
     * MERGE INTO dest
     * USING ( SELECT "source".id,
     *                "source"."key",
     *                "source"."value",
     *                "source".optional_value,
     *                "source"."at"
     *           FROM "source"
     *          WHERE "source"."key" = 'only-in-source-1'
     *       ) as sub ON dest."key" = sub."key"
     *  WHEN NOT MATCHED THEN
     *      INSERT ("at", "key", "value")
     *      VALUES ('2000-01-01T00:00:00', sub."key", sub."value")
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mergeFrom with const condition`(testDB: TestDB) {
        val filteredSourceQuery = Source.selectAll()
            .where { Source.key eq "only-in-source-1" }
            .alias("sub")

        withMergeTestTablesAndDefaultData(testDB) { dest, source ->
            dest.mergeFrom(
                filteredSourceQuery,
                on = { Dest.key eq filteredSourceQuery[Source.key] },
            ) {
                whenNotMatchedInsert {
                    it[dest.key] = filteredSourceQuery[source.key]
                    it[dest.value] = filteredSourceQuery[source.value]
                }
            }

            dest.getByKey("only-in-source-1")[dest.value] shouldBeEqualTo 1

            dest.selectAll()
                .where { Dest.key eq "only-in-source-2" }
                .firstOrNull().shouldBeNull()

            /**
             * `exists` 를 사용하는게 더 성능을 높힐 수 있습니다. (vs count, selectAll 보다)
             * ```sql
             * SELECT EXISTS (SELECT dest.id FROM dest WHERE dest."key" = 'only-in-source-2')
             * ```
             */
            val existsExpr = exists(
                dest.select(dest.id)
                    .where { dest.key eq "only-in-source-2" }
            )
            Dual.select(existsExpr).first()[existsExpr].shouldBeFalse()
        }
    }
}
