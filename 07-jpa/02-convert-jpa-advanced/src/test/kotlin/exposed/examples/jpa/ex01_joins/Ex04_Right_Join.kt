package exposed.examples.jpa.ex01_joins

import exposed.shared.mapping.OrderSchema.OrderRecord
import exposed.shared.mapping.OrderSchema.withOrdersTables
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.core.rightJoin
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex04_Right_Join: JdbcExposedTestBase() {

    companion object: KLogging()

    private val expected = listOf(
        OrderRecord(itemId = 55, orderId = null, quantity = null, description = "Catcher Glove"),
        OrderRecord(itemId = 22, orderId = 1, quantity = 1, description = "Helmet"),
        OrderRecord(itemId = 33, orderId = 1, quantity = 1, description = "First Base Glove"),
        OrderRecord(itemId = 22, orderId = 2, quantity = 1, description = "Helmet"),
        OrderRecord(itemId = 44, orderId = 2, quantity = 1, description = "Outfield Glove")
    )

    /**
     * Right join with alias
     *
     * ```sql
     * -- Postgres:
     * SELECT om.id,
     *        ol.quantity,
     *        im.id,
     *        im.description
     *   FROM orders om
     *      INNER JOIN order_lines ol ON (om.id = ol.order_id)
     *      RIGHT JOIN items im ON (ol.item_id = im.id)
     *  ORDER BY om.id ASC NULLS FIRST,
     *           im.id ASC NULLS FIRST
     * ```
     * ```sql
     * -- MySQL V8:
     * SELECT om.id,
     *        ol.quantity,
     *        im.id,
     *        im.description
     *   FROM orders om
     *      INNER JOIN order_lines ol ON (om.id = ol.order_id)
     *      RIGHT JOIN items im ON (ol.item_id = im.id)
     *  ORDER BY om.id ASC,
     *           im.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `right join with aliases`(testDB: TestDB) {
        withOrdersTables(testDB) { orders, _, items, orderLines, _ ->

            val om = orders.alias("om")
            val ol = orderLines.alias("ol")
            val im = items.alias("im")

            val slice = listOf(
                om[orders.id], // orderIdAlias,
                ol[orderLines.quantity],
                im[items.id], // itemIdAlias,
                im[items.description]
            )

            val rightJoin = om
                .innerJoin(ol) { om[orders.id] eq ol[orderLines.orderId] }
                .rightJoin(im) { ol[orderLines.itemId] eq im[items.id] }

            val records = rightJoin
                .select(slice)
                .orderBy(om[orders.id], SortOrder.ASC_NULLS_FIRST)
                .orderBy(im[items.id], SortOrder.ASC_NULLS_FIRST)
                .map {
                    OrderRecord(
                        itemId = it[im[items.id]]?.value,
                        orderId = it[om[orders.id]]?.value,
                        quantity = it[ol[orderLines.quantity]],
                        description = it[im[items.description]]
                    )
                }

            records.forEach {
                log.debug { it }
            }

            records shouldHaveSize expected.size
            records shouldBeEqualTo expected
        }
    }

    /**
     * Right join with subqueries
     *
     * ```sql
     * -- Postgres
     * SELECT om.id,
     *        ol.quantity,
     *        im.id,
     *        im.description
     *   FROM (SELECT orders.id, orders.order_date FROM orders) om
     *      INNER JOIN (SELECT order_lines.order_id,
     *                         order_lines.item_id,
     *                         order_lines.quantity
     *                    FROM order_lines) ol ON (om.id = ol.order_id)
     *      RIGHT JOIN (SELECT items.id,
     *                         items.description
     *                    FROM items) im ON  (ol.item_id = im.id)
     *  ORDER BY om.id ASC NULLS FIRST,
     *           im.id ASC NULLS FIRST
     * ```
     * ```sql
     * -- MySQL V8
     * SELECT om.id,
     *        ol.quantity,
     *        im.id,
     *        im.description
     *   FROM (SELECT orders.id, orders.order_date FROM orders) om
     *      INNER JOIN (SELECT order_lines.order_id,
     *                         order_lines.item_id,
     *                         order_lines.quantity
     *                    FROM order_lines) ol ON (om.id = ol.order_id)
     *      RIGHT JOIN (SELECT items.id,
     *                         items.description
     *                    FROM items) im ON (ol.item_id = im.id)
     *  ORDER BY om.id ASC,
     *           im.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `right join with subqueries`(testDB: TestDB) {
        withOrdersTables(testDB) { orders, _, items, orderLines, _ ->

            val om = orders.select(orders.id, orders.orderDate).alias("om")
            val ol = orderLines.select(orderLines.orderId, orderLines.itemId, orderLines.quantity).alias("ol")
            val im = items.select(items.id, items.description).alias("im")

            val slice = listOf(
                om[orders.id], // orderIdAlias,
                ol[orderLines.quantity],
                im[items.id], // itemIdAlias,
                im[items.description]
            )

            val rightJoin = om
                .innerJoin(ol) { om[orders.id] eq ol[orderLines.orderId] }
                .rightJoin(im) { ol[orderLines.itemId] eq im[items.id] }

            val records = rightJoin
                .select(slice)
                .orderBy(om[orders.id], SortOrder.ASC_NULLS_FIRST)
                .orderBy(im[items.id], SortOrder.ASC_NULLS_FIRST)
                .map {
                    OrderRecord(
                        itemId = it[im[items.id]]?.value,
                        orderId = it[om[orders.id]]?.value,
                        quantity = it[ol[orderLines.quantity]],
                        description = it[im[items.description]]
                    )
                }

            records.forEach {
                log.debug { it }
            }

            records shouldHaveSize expected.size
            records shouldBeEqualTo expected
        }
    }

    /**
     * Right Join example
     *
     * ```sql
     * -- Postgres
     * SELECT orders.id,
     *        order_lines.quantity,
     *        items.id,
     *        items.description
     *   FROM orders
     *      INNER JOIN order_lines ON (orders.id = order_lines.order_id)
     *      RIGHT JOIN items ON (order_lines.item_id = items.id)
     *  ORDER BY orders.id ASC NULLS FIRST,
     *           items.id ASC NULLS FIRST;
     * ```
     * ```sql
     * SELECT orders.id,
     *        order_lines.quantity,
     *        items.id,
     *        items.description
     *   FROM orders
     *      INNER JOIN order_lines ON (orders.id = order_lines.order_id)
     *      RIGHT JOIN items ON (order_lines.item_id = items.id)
     *  ORDER BY orders.id ASC,
     *           items.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `right join without aliases`(testDB: TestDB) {
        withOrdersTables(testDB) { orders, _, items, orderLines, _ ->

            val slice = listOf(
                orders.id,
                orderLines.quantity,
                items.id,
                items.description
            )

            val rightJoin = orders
                .innerJoin(orderLines) { orders.id eq orderLines.orderId }
                .rightJoin(items) { orderLines.itemId eq items.id }

            val records = rightJoin
                .select(slice)
                .orderBy(orders.id, SortOrder.ASC_NULLS_FIRST)
                .orderBy(items.id, SortOrder.ASC_NULLS_FIRST)
                .map {
                    OrderRecord(
                        itemId = it[items.id]?.value,
                        orderId = it[orders.id]?.value,
                        quantity = it[orderLines.quantity],
                        description = it[items.description]
                    )
                }

            records.forEach {
                log.debug { it }
            }

            records shouldHaveSize expected.size
            records shouldBeEqualTo expected
        }
    }
}
