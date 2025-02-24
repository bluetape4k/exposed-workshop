package exposed.dml.example

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * `DISTINCT ON` (`withDistinctOn`) 을 사용하는 예제입니다.
 *
 * Postgres와 H2 에서만 지원됩니다. MySQL과 MariaDB에서는 지원되지 않습니다.
 */
class Ex07_DistinctOn: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * `withDistinctOn` (`DISTINCT ON`) 은 Postgres와 H2 에서민 지원됩니다.
     */
    private val distinctOnSupportedDb = TestDB.ALL_POSTGRES + TestDB.ALL_H2

    /**
     * `withDistinctOn` (`DISTINCT ON`) 은 Postgres와 H2 에서민 지원됩니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `distinctOn method`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in distinctOnSupportedDb }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      id SERIAL PRIMARY KEY,
         *      v1 INT NOT NULL,
         *      v2 INT NOT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("tester") {
            val v1 = integer("v1")
            val v2 = integer("v2")
        }

        withTables(testDB, tester) {
            tester.batchInsert(
                listOf(
                    listOf(1, 1), listOf(1, 2), listOf(1, 2),
                    listOf(2, 1), listOf(2, 2), listOf(2, 2),
                    listOf(4, 4), listOf(4, 4), listOf(4, 4),
                )
            ) {
                this[tester.v1] = it[0]
                this[tester.v2] = it[1]
            }
            /**
             * `DISTINCT ON (tester.v1)` 을 사용한 쿼리입니다.
             *
             * ```sql
             * -- Postgres
             * SELECT DISTINCT ON (tester.v1)
             *        tester.id,
             *        tester.v1,
             *        tester.v2
             *   FROM tester
             *  ORDER BY tester.v1 ASC,
             *           tester.v2 ASC
             *  ```
             */
            val distinctValue1 = tester.selectAll()
                .withDistinctOn(tester.v1)
                .orderBy(tester.v1 to ASC, tester.v2 to ASC)
                .map { it[tester.v1] to it[tester.v2] }

            distinctValue1 shouldBeEqualTo listOf(1 to 1, 2 to 1, 4 to 4)

            /**
             * `DISTINCT ON (tester.v2)` 을 사용한 쿼리입니다.
             *
             * ```sql
             * -- Postgres
             * SELECT DISTINCT ON (tester.v2)
             *        tester.id,
             *        tester.v1,
             *        tester.v2
             *   FROM tester
             *  ORDER BY tester.v2 ASC,
             *           tester.v1 ASC
             *  ```
             */
            val distinctValue2 = tester.selectAll()
                .withDistinctOn(tester.v2)
                .orderBy(tester.v2 to ASC, tester.v1 to ASC)
                .map { it[tester.v1] to it[tester.v2] }

            distinctValue2 shouldBeEqualTo listOf(1 to 1, 1 to 2, 4 to 4)

            /**
             * `DISTINCT ON (tester.v1, tester.v2)` 을 사용한 쿼리입니다.
             *
             *  ```sql
             *  -- Postgres
             *  SELECT DISTINCT ON (tester.v1, tester.v2)
             *         tester.id,
             *         tester.v1,
             *         tester.v2
             *    FROM tester
             *   ORDER BY tester.v1 ASC,
             *            tester.v2 ASC
             *  ```
             */
            val distinctBoth = tester.selectAll()
                .withDistinctOn(tester.v1, tester.v2)
                .orderBy(tester.v1 to ASC, tester.v2 to ASC)
                .map { it[tester.v1] to it[tester.v2] }

            distinctBoth shouldBeEqualTo listOf(1 to 1, 1 to 2, 2 to 1, 2 to 2, 4 to 4)

            /**
             * `DISTINCT ON (tester.v1, tester.v2)` 을 정렬 방식과 같이 사용할 수 있다
             *
             * ```sql
             * -- Postgres
             * SELECT DISTINCT ON (tester.v1, tester.v2)
             *        tester.id,
             *        tester.v1,
             *        tester.v2
             *   FROM tester
             *  ORDER BY tester.v1 ASC,
             *           tester.v2 ASC
             * ```
             */
            val distinctSequential = tester.selectAll()
                .withDistinctOn(tester.v1 to ASC)
                .withDistinctOn(tester.v2 to ASC)
                .map { it[tester.v1] to it[tester.v2] }

            distinctSequential shouldBeEqualTo distinctBoth
        }
    }
}
