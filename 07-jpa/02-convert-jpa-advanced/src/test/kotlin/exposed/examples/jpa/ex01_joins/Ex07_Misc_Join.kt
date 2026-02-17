package exposed.examples.jpa.ex01_joins

import com.impossibl.postgres.jdbc.PGSQLSimpleException
import exposed.shared.mapping.OrderSchema.withOrdersTables
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.eqSubQuery
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class Ex07_Misc_Join: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * Join 시 조건을 주지 않으면 에러가 발생한다
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `join with no condition`(testDB: TestDB) {
        withOrdersTables(testDB) { _, _, _, _, users ->
            val u2 = users.alias("u2")
            val u3 = users.alias("u3")

            // u3 는 조인 조건이 없다
            val query = users
                .innerJoin(u2) { users.id eq u3[users.id] }
                .selectAll()
                .where { u2[users.id] eq 4L }

            if (testDB == TestDB.POSTGRESQLNG) {
                assertFailsWith<PGSQLSimpleException> {
                    query.toList()
                }
            } else {
                assertFailsWith<ExposedSQLException> {
                    query.toList()
                }
            }
        }
    }

    /**
     * `eqSubQuery` 를 사용하여 Where 조건을 지정한다
     *
     * ```sql
     * -- Postgres
     * SELECT ol1.order_id,
     *        ol1.line_number
     *   FROM order_lines ol1
     *  WHERE ol1.line_number = (SELECT MAX(ol2.line_number)
     *                             FROM order_lines ol2
     *                            WHERE ol2.order_id = ol1.order_id)
     *  ORDER BY ol1.id ASC
     * ```
     *
     * ```sql
     * -- MySQL V8
     * SELECT ol1.order_id,
     *        ol1.line_number
     *   FROM order_lines ol1
     *  WHERE ol1.line_number = (SELECT MAX(ol2.line_number)
     *                             FROM order_lines ol2
     *                            WHERE ol2.order_id = ol1.order_id)
     *  ORDER BY ol1.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `aliases propagate to subquery condition`(testDB: TestDB) {
        withOrdersTables(testDB) { _, _, _, orderLines, _ ->
            val ol1 = orderLines.alias("ol1")
            val ol2 = orderLines.alias("ol2")

            val rows = ol1.select(ol1[orderLines.orderId], ol1[orderLines.lineNumber])
                .where {
                    ol1[orderLines.lineNumber] eqSubQuery
                            ol2.select(ol2[orderLines.lineNumber].max())
                                .where { ol2[orderLines.orderId] eq ol1[orderLines.orderId] }
                }
                .orderBy(ol1[orderLines.id])
                .toList()

            // orderId=1, line number=2
            // orderId=2, line number=3
            rows.forEach { row ->
                log.debug { "orderId=${row[ol1[orderLines.orderId]]}, line number=${row[ol1[orderLines.lineNumber]]}" }
            }
            rows shouldHaveSize 2
        }
    }
}
