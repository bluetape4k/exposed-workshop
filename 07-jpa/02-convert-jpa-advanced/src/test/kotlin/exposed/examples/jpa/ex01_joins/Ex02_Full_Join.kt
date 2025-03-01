package exposed.examples.jpa.ex01_joins

import exposed.shared.mapping.OrderSchema.OrderRecord
import exposed.shared.mapping.OrderSchema.withOrdersTables
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.rightJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.union
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * // NOTE: H2, MySQL 는 FULL JOIN 을 지원하지 않는다. LEFT JOIN 과 RIGHT JOIN 을 UNION 한다
 *
 * [mysql에서 full outer join 사용하기](https://wkdgusdn3.tistory.com/entry/mysql%EC%97%90%EC%84%9C-full-outer-join-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0)
 */
class Ex02_Full_Join: AbstractExposedTest() {

    companion object: KLogging()

    private val expected = listOf(
        OrderRecord(itemId = 55, orderId = null, quantity = null, description = "Catcher Glove"),
        OrderRecord(itemId = 22, orderId = 1, quantity = 1, description = "Helmet"),
        OrderRecord(itemId = 33, orderId = 1, quantity = 1, description = "First Base Glove"),
        OrderRecord(itemId = null, orderId = 2, quantity = 6, description = null),
        OrderRecord(itemId = 22, orderId = 2, quantity = 1, description = "Helmet"),
        OrderRecord(itemId = 44, orderId = 2, quantity = 1, description = "Outfield Glove")
    )

    /**
     * Full Join 을 지원하지 않는 경우, LEFT JOIN 과 RIGHT JOIN 을 UNION 한다
     *
     * ```sql
     * -- Postgres
     * SELECT om.id order_id,
     *        ol.quantity,
     *        im.id item_id,
     *        im.description
     *   FROM orders om
     *      INNER JOIN order_lines ol ON (om.id = ol.order_id)
     *       LEFT JOIN items im ON (ol.item_id = im.id)
     *
     *  UNION
     *
     * SELECT om.id order_id,
     *        ol.quantity,
     *        im.id item_id,
     *        im.description
     *   FROM orders om
     *      INNER JOIN order_lines ol ON (om.id = ol.order_id)
     *      RIGHT JOIN items im ON (ol.item_id = im.id)
     *
     *  ORDER BY order_id ASC NULLS FIRST,
     *           item_id ASC NULLS FIRST
     * ```
     *  ```sql
     *  -- MySQL V8
     *  SELECT om.ID order_id,
     *         ol.QUANTITY,
     *         im.ID item_id,
     *         im.DESCRIPTION
     *    FROM ORDERS om
     *          INNER JOIN ORDER_LINES ol ON (om.ID = ol.ORDER_ID)
     *          LEFT JOIN ITEMS im ON (ol.ITEM_ID = im.ID)
     *
     * UNION
     *
     * SELECT om.ID order_id,
     *        ol.QUANTITY,
     *        im.ID item_id,
     *        im.DESCRIPTION
     *   FROM ORDERS om
     *          INNER JOIN ORDER_LINES ol ON (om.ID = ol.ORDER_ID)
     *          RIGHT JOIN ITEMS im ON (ol.ITEM_ID = im.ID)
     *
     *  ORDER BY order_id ASC,
     *           item_id ASC
     *  ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `full join with aliases`(testDB: TestDB) {
        withOrdersTables(testDB) { orders, _, items, orderLines, _ ->

            val om = orders.alias("om")
            val ol = orderLines.alias("ol")
            val im = items.alias("im")

            val orderIdAlias = om[orders.id].alias("order_id")
            val itemIdAlias = im[items.id].alias("item_id")

            val slice = listOf(
                orderIdAlias,
                ol[orderLines.quantity],
                itemIdAlias,
                im[items.description]
            )

            val leftJoin = om
                .innerJoin(ol) { om[orders.id] eq ol[orderLines.orderId] }
                .leftJoin(im) { ol[orderLines.itemId] eq im[items.id] }

            val rightJoin = om
                .innerJoin(ol) { om[orders.id] eq ol[orderLines.orderId] }
                .rightJoin(im) { ol[orderLines.itemId] eq im[items.id] }


            val records = leftJoin.select(slice).union(rightJoin.select(slice))
                .orderBy(orderIdAlias, SortOrder.ASC_NULLS_FIRST)
                .orderBy(itemIdAlias, SortOrder.ASC_NULLS_FIRST)
                .map {
                    OrderRecord(
                        itemId = it[itemIdAlias]?.value,
                        orderId = it[orderIdAlias]?.value,
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
     * Full Join 을 지원하지 않는 경우, LEFT JOIN 과 RIGHT JOIN 을 UNION 한다
     *
     * ```sql
     * -- Postgres
     * SELECT om.id order_id,
     *        ol.quantity,
     *        im.id item_id,
     *        im.description
     *   FROM (SELECT orders.id, orders.order_date FROM orders) om
     *      INNER JOIN (SELECT order_lines.id,
     *                         order_lines.order_id,
     *                         order_lines.item_id,
     *                         order_lines.line_number,
     *                         order_lines.quantity
     *                    FROM order_lines) ol ON  (om.id = ol.order_id)
     *       LEFT JOIN (SELECT items.id,
     *                         items.description
     *                    FROM items) im ON  (ol.item_id = im.id)
     *
     *  UNION
     *
     * SELECT om.id order_id,
     *        ol.quantity,
     *        im.id item_id,
     *        im.description
     *   FROM (SELECT orders.id, orders.order_date FROM orders) om
     *      INNER JOIN (SELECT order_lines.id,
     *                         order_lines.order_id,
     *                         order_lines.item_id,
     *                         order_lines.line_number,
     *                         order_lines.quantity
     *                    FROM order_lines) ol ON  (om.id = ol.order_id)
     *      RIGHT JOIN (SELECT items.id,
     *                         items.description
     *                    FROM items) im ON  (ol.item_id = im.id)
     *
     *   ORDER BY order_id ASC NULLS FIRST,
     *            item_id ASC NULLS FIRST
     * ```
     *
     * ```sql
     * -- MySQL V8
     * SELECT om.ID order_id,
     *        ol.QUANTITY,
     *        im.ID item_id,
     *        im.DESCRIPTION
     *   FROM (SELECT ORDERS.ID, ORDERS.ORDER_DATE FROM ORDERS) om
     *      INNER JOIN (SELECT ORDER_LINES.ID,
     *                         ORDER_LINES.ORDER_ID,
     *                         ORDER_LINES.ITEM_ID,
     *                         ORDER_LINES.LINE_NUMBER,
     *                         ORDER_LINES.QUANTITY
     *                    FROM ORDER_LINES) ol ON  (om.ID = ol.ORDER_ID)
     *      LEFT JOIN (SELECT ITEMS.ID,
     *                        ITEMS.DESCRIPTION
     *                   FROM ITEMS) im ON  (ol.ITEM_ID = im.ID)
     *
     * UNION
     *
     * SELECT om.ID order_id,
     *        ol.QUANTITY,
     *        im.ID item_id,
     *        im.DESCRIPTION
     *   FROM (SELECT ORDERS.ID, ORDERS.ORDER_DATE FROM ORDERS) om
     *      INNER JOIN (SELECT ORDER_LINES.ID,
     *                         ORDER_LINES.ORDER_ID,
     *                         ORDER_LINES.ITEM_ID,
     *                         ORDER_LINES.LINE_NUMBER,
     *                         ORDER_LINES.QUANTITY
     *                    FROM ORDER_LINES) ol ON (om.ID = ol.ORDER_ID)
     *      RIGHT JOIN (SELECT ITEMS.ID,
     *                         ITEMS.DESCRIPTION
     *                    FROM ITEMS) im ON (ol.ITEM_ID = im.ID)
     *
     * ORDER BY order_id ASC NULLS FIRST,
     *          item_id ASC NULLS FIRST
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `full join with subquery`(testDB: TestDB) {
        withOrdersTables(testDB) { orders, _, items, orderLines, _ ->

            val om = orders.selectAll().alias("om")
            val ol = orderLines.selectAll().alias("ol")
            val im = items.selectAll().alias("im")

            // Ordering 을 위해 alias 를 사용한다
            val orderIdAlias = om[orders.id].alias("order_id")
            val itemIdAlias = im[items.id].alias("item_id")

            val slice = listOf(
                orderIdAlias,
                ol[orderLines.quantity],
                itemIdAlias,
                im[items.description]
            )

            val leftJoin = om
                .innerJoin(ol) { om[orders.id] eq ol[orderLines.orderId] }
                .leftJoin(im) { ol[orderLines.itemId] eq im[items.id] }

            val rightJoin = om
                .innerJoin(ol) { om[orders.id] eq ol[orderLines.orderId] }
                .rightJoin(im) { ol[orderLines.itemId] eq im[items.id] }

            val records = leftJoin
                .select(slice)
                .union(rightJoin.select(slice))
                .orderBy(orderIdAlias, SortOrder.ASC_NULLS_FIRST)
                .orderBy(itemIdAlias, SortOrder.ASC_NULLS_FIRST)
                .map {
                    OrderRecord(
                        itemId = it[itemIdAlias]?.value,
                        orderId = it[orderIdAlias]?.value,
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
     * Full Join 을 지원하지 않는 경우, LEFT JOIN 과 RIGHT JOIN 을 UNION 한다
     *
     * ```sql
     * -- Postgres
     * SELECT orders.id order_id,
     *        order_lines.quantity,
     *        items.id item_id,
     *        items.description
     *   FROM orders
     *      INNER JOIN order_lines ON (orders.id = order_lines.order_id)
     *      LEFT JOIN items ON (order_lines.item_id = items.id)
     *
     *  UNION
     *
     * SELECT orders.id order_id,
     *        order_lines.quantity,
     *        items.id item_id,
     *        items.description
     *   FROM orders
     *      INNER JOIN order_lines ON (orders.id = order_lines.order_id)
     *      RIGHT JOIN items ON (order_lines.item_id = items.id)
     *
     *  ORDER BY order_id ASC NULLS FIRST,
     *           item_id ASC NULLS FIRST
     * ```
     *
     * ```sql
     * -- MySQL V8
     * SELECT ORDERS.ID order_id,
     *        ORDER_LINES.QUANTITY,
     *        ITEMS.ID item_id,
     *        ITEMS.DESCRIPTION
     *   FROM ORDERS
     *      INNER JOIN ORDER_LINES ON (ORDERS.ID = ORDER_LINES.ORDER_ID)
     *      LEFT JOIN ITEMS ON (ORDER_LINES.ITEM_ID = ITEMS.ID)
     *
     * UNION
     *
     * SELECT ORDERS.ID order_id,
     *        ORDER_LINES.QUANTITY,
     *        ITEMS.ID item_id,
     *        ITEMS.DESCRIPTION
     *   FROM ORDERS
     *      INNER JOIN ORDER_LINES ON (ORDERS.ID = ORDER_LINES.ORDER_ID)
     *      RIGHT JOIN ITEMS ON (ORDER_LINES.ITEM_ID = ITEMS.ID)
     *
     *  ORDER BY order_id ASC NULLS FIRST,
     *           item_id ASC NULLS FIRST
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `full join without aliases`(testDB: TestDB) {
        withOrdersTables(testDB) { orders, _, items, orderLines, _ ->

            val leftJoin = orders
                .innerJoin(orderLines) { orders.id eq orderLines.orderId }
                .leftJoin(items) { orderLines.itemId eq items.id }

            val rightJoin = orders
                .innerJoin(orderLines) { orders.id eq orderLines.orderId }
                .rightJoin(items) { orderLines.itemId eq items.id }

            // Ordering 을 위해 alias 를 사용한다
            val orderIdAlias = orders.id.alias("order_id")
            val itemIdAlias = items.id.alias("item_id")


            val slice = listOf(
                orderIdAlias,
                orderLines.quantity,
                itemIdAlias,
                items.description
            )

            val records = leftJoin
                .select(slice)
                .union(rightJoin.select(slice))
                .orderBy(orderIdAlias, SortOrder.ASC_NULLS_FIRST)
                .orderBy(itemIdAlias, SortOrder.ASC_NULLS_FIRST)
                .map {
                    OrderRecord(
                        itemId = it[itemIdAlias]?.value,
                        orderId = it[orderIdAlias]?.value,
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
