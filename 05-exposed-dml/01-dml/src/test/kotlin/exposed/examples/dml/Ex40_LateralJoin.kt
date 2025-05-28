package exposed.examples.dml

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.expectException
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.joinQuery
import org.jetbrains.exposed.v1.core.lastQueryAlias
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import org.junit.jupiter.params.provider.MethodSource

/**
 * Lateral Join Query 예제 모음
 *
 * LATERAL은 SQL에서 서브쿼리가 메인 쿼리의 컬럼을 참조할 수 있도록 해주는 기능입니다. 이 기능은 주로 LATERAL JOIN에서 사용되어, 메인 쿼리의 각 행에 대해 서브쿼리를 실행하고 그 결과를 조인하는 방식으로 작동합니다.
 *
 * ### LATERAL의 기능
 * 1. 서브쿼리와 메인 쿼리의 상관관계 유지:
 *   * LATERAL을 사용하면 서브쿼리가 메인 쿼리의 현재 행에 있는 값을 참조할 수 있습니다.
 *     이는 일반적인 서브쿼리와 달리, 메인 쿼리의 컨텍스트를 유지하면서 동작할 수 있게 합니다12.
 *
 * 2. 유연한 데이터 조회:
 *   * LATERAL JOIN은 각 행마다 서브쿼리를 실행하기 때문에, 특정 조건에 맞는 데이터를 유연하게 조회할 수 있습니다.
 *     예를 들어, 각 고객의 위시리스트에 따라 해당 고객이 원하는 가격 이하의 상품을 찾는 쿼리를 작성할 수 있습니다3.
 * 3. 조인의 출력 제어:
 *   * LATERAL 조인은 인라인 뷰에서 생성된 행만 포함되며, 메인 테이블의 행이 반드시 조인될 필요가 없습니다.
 *     이는 LATERAL이 서브쿼리 내에서 메인 테이블의 데이터를 처리할 수 있도록 해주기 때문입니다2.
 *
 * 참고: [Lateral 기능](https://www.perplexity.ai/search/sql-joinsi-lateral-yi-gineunge-dEWB59TDTqOYIIW0bB86pQ)
 */
class Ex40_LateralJoin: JdbcExposedTestBase() {

    companion object: KLogging()

    private val lateralJoinSupportedDb = TestDB.ALL_POSTGRES


    /**
     * ### Lateral Join Query
     *
     * ```sql
     * -- Postgres
     * SELECT parent.id,
     *        parent."value",
     *        q0.id,
     *        q0.parent_id,
     *        q0."value"
     *   FROM parent CROSS JOIN LATERAL
     *          (SELECT child.id,
     *                  child.parent_id,
     *                  child."value"
     *             FROM child
     *            WHERE child."value" > parent."value"
     *            LIMIT 1
     *          ) q0
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `lateral join query`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in lateralJoinSupportedDb }

        withTestTablesAndDefaultData(testDB) { parent, child ->
            // Lateral Join 을 위해서 subQuery를 생성합니다.
            val query: Join = parent.joinQuery(joinType = JoinType.CROSS, lateral = true) {
                child.selectAll().where { child.value greater parent.value }.limit(1)
            }

            val subqueryAlias = query.lastQueryAlias ?: error("Alias must exist!")

            val rows = query.selectAll().toList()
            rows.map { it[subqueryAlias[child.value]] } shouldBeEqualTo listOf(30)
            rows.forEach {
                log.debug { "parent.id=${it[parent.id]}, parent.value=${it[parent.value]}, child.value=${it[subqueryAlias[child.value]]}" }
            }
        }
    }

    /**
     * ### Lateral Join Query Alias
     *
     * #### Cross Join Lateral Query
     * ```sql
     * SELECT parent.id,
     *        parent."value",
     *        child1.id,
     *        child1.parent_id,
     *        child1."value"
     *   FROM parent CROSS JOIN LATERAL
     *          (SELECT child.id,
     *                  child.parent_id,
     *                  child."value"
     *             FROM child
     *            WHERE child."value" > parent."value"
     *            LIMIT 1
     *          ) child1
     * ```
     * #### Left Join Lateral Query
     * ```sql
     * SELECT parent.id,
     *        parent."value",
     *        child1.id,
     *        child1.parent_id,
     *        child1."value"
     *   FROM parent LEFT JOIN LATERAL
     *          (SELECT child.id,
     *                  child.parent_id,
     *                  child."value"
     *             FROM child
     *            WHERE child."value" > parent."value"
     *          ) child1 ON parent.id = child1.parent_id
     * ```
     *
     * #### Left join to Alias
     * ```sql
     * SELECT parent1.id,
     *        parent1."value",
     *        child1.id,
     *        child1.parent_id,
     *        child1."value"
     *   FROM (SELECT parent.id,
     *                parent."value"
     *           FROM parent
     *        ) parent1
     *         LEFT JOIN LATERAL
     *         (SELECT child.id,
     *                 child.parent_id,
     *                 child."value"
     *            FROM child
     *           WHERE child."value" > parent1."value"
     *         ) child1
     *         ON parent1.id = child1.parent_id
     * ```
     */
    @ParameterizedTest
    @FieldSource("lateralJoinSupportedDb")
    fun `lateral join query alias`(dialect: TestDB) {
        withTestTablesAndDefaultData(dialect) { parent, child ->

            child.selectAll()
                .where { child.value greater parent.value }
                .limit(1)
                .alias("child1")
                .let { subqueryAlias ->
                    val join = parent.join(
                        subqueryAlias,
                        JoinType.CROSS,
                        onColumn = parent.id,
                        otherColumn = subqueryAlias[child.parent],
                        lateral = true
                    )

                    join.selectAll().map { it[subqueryAlias[child.value]] } shouldBeEqualTo listOf(30)
                }

            child.selectAll()
                .where { child.value greater parent.value }
                .alias("child1")
                .let { subqueryAlias ->
                    val join = parent.join(
                        subqueryAlias,
                        JoinType.LEFT,
                        onColumn = parent.id,
                        otherColumn = subqueryAlias[child.parent],
                        lateral = true
                    )

                    join.selectAll().map { it[subqueryAlias[child.value]] } shouldBeEqualTo listOf(30)
                }

            val parentQuery = parent.selectAll().alias("parent1")
            child.selectAll()
                .where { child.value greater parentQuery[parent.value] }
                .alias("child1")
                .let { subqueryAlias ->
                    val join: Join = parentQuery.join(
                        subqueryAlias,
                        JoinType.LEFT,
                        onColumn = parentQuery[parent.id],
                        otherColumn = subqueryAlias[child.parent],
                        lateral = true
                    )

                    join.selectAll().map { it[subqueryAlias[child.value]] } shouldBeEqualTo listOf(30)
                }
        }
    }

    /**
     * ### Lateral Join 시 주의해야 할 점
     *
     * 1. Lateral 적용 시, 명시적으로 테이블 간의 JOIN 조건의 컬럼을 지정하면 예외가 발생합니다. (쿼리를 사용해야 합니다)
     * 2. Lateral 적용 시, 암묵적인 테이블 간의 JOIN 조건의 컬럼을 지정하면 예외가 발생합니다. (쿼리를 사용해야 합니다)
     */
    @ParameterizedTest
    @FieldSource("lateralJoinSupportedDb")
    fun `lateral direct table join`(dialect: TestDB) {
        withTestTables(dialect) { parent, child ->
            // Lateral 적용 시, 명시적으로 테이블 간의 JOIN 조건의 컬럼을 지정하면 예외가 발생합니다. (쿼리를 사용해야 합니다)
            expectException<IllegalArgumentException> {
                parent.join(child, JoinType.LEFT, onColumn = parent.id, otherColumn = child.parent, lateral = true)
            }

            // Lateral 적용 시, 암묵적인 테이블 간의 JOIN 조건의 컬럼을 지정하면 예외가 발생합니다. (쿼리를 사용해야 합니다)
            expectException<IllegalArgumentException> {
                parent.join(child, JoinType.LEFT, lateral = true).selectAll().toList()
            }
        }
    }

    /**
     * Parent Table
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS parent (
     *      id SERIAL PRIMARY KEY,
     *      "value" INT NOT NULL
     * )
     * ```
     */
    object Parent: IntIdTable("parent") {
        val value = integer("value")
    }

    /**
     * Child Table
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS child (
     *      id SERIAL PRIMARY KEY,
     *      parent_id INT NOT NULL,
     *      "value" INT NOT NULL,
     *
     *      CONSTRAINT fk_child_parent_id__id
     *          FOREIGN KEY (parent_id) REFERENCES parent(id)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object Child: IntIdTable("child") {
        val parent = reference("parent_id", Parent.id)
        val value = integer("value")
    }

    private fun withTestTables(testDB: TestDB, statement: JdbcTransaction.(Parent, Child) -> Unit) {
        withTables(testDB, Parent, Child) {
            statement(Parent, Child)
        }
    }

    private fun withTestTablesAndDefaultData(
        testDB: TestDB,
        statement: JdbcTransaction.(Parent, Child) -> Unit,
    ) {
        withTestTables(testDB) { parent, child ->
            val id = parent.insertAndGetId { it[value] = 20 }

            child.batchInsert(listOf(10, 30)) { value ->
                this[child.parent] = id
                this[child.value] = value
            }

            statement(parent, child)
        }
    }
}
