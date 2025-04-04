package exposed.examples.dml

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Assumptions
import java.time.LocalDateTime

val TEST_DEFAULT_DATE_TIME: LocalDateTime = LocalDateTime.of(
    2000,
    1,
    1,
    0,
    0,
    0,
    0
)

/**
 * `MERGE INTO` 구문은 데이터베이스에서 조건에 따라 데이터를 삽입, 갱신, 삭제하는 작업을 한 번에 수행할 수 있게 해주는 강력한 기능입니다
 *
 * 참고: [SQL MERGE INTO 설명](https://www.perplexity.ai/search/sql-merge-into-gumune-daehae-s-y_xKDfwFR8ewN6qIY9jqJw)
 */
abstract class Ex14_MergeBase: AbstractExposedTest() {


    companion object: KLogging()

    protected fun allDbExcept(includeSettings: Collection<TestDB>) = TestDB.ALL - includeSettings.toSet()

    protected val notSupportsMergeInto = TestDB.ALL_MYSQL_MARIADB + TestDB.ALL_H2_V1

    protected fun withMergeTestTables(
        testDB: TestDB,
        statement: Transaction.(dest: Dest, source: Source) -> Unit,
    ) {
        Assumptions.assumeTrue(testDB !in notSupportsMergeInto)

        withTables(testDB, Source, Dest) {
            statement(Dest, Source)
        }
    }

    protected fun withMergeTestTablesAndDefaultData(
        testDB: TestDB,
        statement: Transaction.(dest: Dest, source: Source) -> Unit,
    ) {
        withMergeTestTables(testDB) { dest, source ->
            source.insert(key = "only-in-source-1", value = 1)
            source.insert(key = "only-in-source-2", value = 2)
            source.insert(key = "only-in-source-3", value = 3, optional = "optional-is-present")
            source.insert(key = "only-in-source-4", value = 4, at = LocalDateTime.of(2050, 1, 1, 0, 0, 0, 0))

            dest.insert(key = "only-in-dest-1", value = 10)
            dest.insert(key = "only-in-dest-2", value = 20)
            dest.insert(key = "only-in-dest-3", value = 30, optional = "optional-is-present")
            dest.insert(key = "only-in-dest-4", value = 40, at = LocalDateTime.of(2050, 1, 1, 0, 0, 0, 0))

            source.insert(key = "in-source-and-dest-1", value = 1)
            dest.insert(key = "in-source-and-dest-1", value = 10)
            source.insert(key = "in-source-and-dest-2", value = 2)
            dest.insert(key = "in-source-and-dest-2", value = 20)
            source.insert(key = "in-source-and-dest-3", value = 3, optional = "optional-is-present")
            dest.insert(key = "in-source-and-dest-3", value = 30, optional = "optional-is-present")
            source.insert(key = "in-source-and-dest-4", value = 4, at = LocalDateTime.of(1950, 1, 1, 0, 0, 0, 0))
            dest.insert(key = "in-source-and-dest-4", value = 40, at = LocalDateTime.of(1950, 1, 1, 0, 0, 0, 0))

            statement(Dest, Source)
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS "source" (
     *      id SERIAL PRIMARY KEY,
     *      "key" VARCHAR(128) NOT NULL,
     *      "value" INT NOT NULL,
     *      optional_value TEXT NULL,
     *      "at" TIMESTAMP NOT NULL
     * )
     * ```
     */
    object Source: IntIdTable("source") {
        val key = varchar("key", 128).uniqueIndex()
        val value = integer("value")
        val optional = text("optional_value").nullable()
        val at = datetime("at").clientDefault { TEST_DEFAULT_DATE_TIME }

        fun insert(key: String, value: Int, optional: String? = null, at: LocalDateTime? = null) {
            Source.insert {
                it[Source.key] = key
                it[Source.value] = value
                optional?.let { optional -> it[Source.optional] = optional }
                at?.let { at -> it[Source.at] = at }
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS dest (
     *      id SERIAL PRIMARY KEY,
     *      "key" VARCHAR(128) NOT NULL,
     *      "value" INT NOT NULL,
     *      optional_value TEXT NULL,
     *      "at" TIMESTAMP NOT NULL
     * )
     * ```
     */
    object Dest: IntIdTable("dest") {
        val key = varchar("key", 128).uniqueIndex()
        val value = integer("value")
        val optional = text("optional_value").nullable()
        val at = datetime("at").clientDefault { TEST_DEFAULT_DATE_TIME }

        fun insert(key: String, value: Int, optional: String? = null, at: LocalDateTime? = null) {
            Dest.insert {
                it[Dest.key] = key
                it[Dest.value] = value
                optional?.let { optional -> it[Dest.optional] = optional }
                at?.let { at -> it[Dest.at] = at }
            }
        }

        fun getByKey(key: String): ResultRow = Dest.selectAll().where { Dest.key eq key }.single()
        fun getByKeyOrNull(key: String): ResultRow? = Dest.selectAll().where { Dest.key eq key }.singleOrNull()
    }
}
