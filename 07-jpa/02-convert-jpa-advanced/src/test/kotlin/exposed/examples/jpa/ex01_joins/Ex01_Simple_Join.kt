package exposed.examples.jpa.ex01_joins

import exposed.shared.mapping.OrderSchema
import exposed.shared.mapping.OrderSchema.withOrdersTables
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex01_Simple_Join: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * Lazy loading by simple join
     *
     * ```sql
     * -- Postgres
     * SELECT om.id,
     *        om.order_date,
     *        od.line_number,
     *        od.description,
     *        od.quantity
     *   FROM orders om
     *      INNER JOIN order_details od ON (om.id = od.order_id)
     *  ORDER BY om.id ASC;
     *
     * SELECT order_details.id,
     *        order_details.order_id,
     *        order_details.line_number,
     *        order_details.description,
     *        order_details.quantity
     *   FROM order_details
     *  WHERE order_details.order_id = 1;
     *
     * SELECT order_details.id,
     *        order_details.order_id,
     *        order_details.line_number,
     *        order_details.description,
     *        order_details.quantity
     *   FROM order_details
     *  WHERE order_details.order_id = 2;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `single table join`(testDB: TestDB) {
        withOrdersTables(testDB) { orders, details, _, _, _ ->
            val om = orders.alias("om")
            val od = details.alias("od")

            val rows = om
                .innerJoin(od) { om[orders.id] eq od[details.orderId] }
                .select(
                    om[orders.id],
                    om[orders.orderDate],
                    od[details.lineNumber],
                    od[details.description],
                    od[details.quantity]
                )
                .orderBy(om[orders.id])
                .toList()

            rows shouldHaveSize 3

            // Entity 로 만들려고 이럴 필요는 없을 것 같다
            val orderList = rows.map { OrderSchema.Order.wrapRow(it) }
            val loadedOrders = orderList.distinctBy { it.id.value }
            loadedOrders shouldHaveSize 2
            loadedOrders.forEach { order ->
                // Lazy loading 이므로 fetching 한다.
                order.details.shouldNotBeEmpty()
            }
        }
    }

    /**
     * Eager loading by simple join
     *
     * ```sql
     * -- Postgres:
     * SELECT orders.id, orders.order_date
     *   FROM orders;
     *
     * SELECT order_details.id,
     *        order_details.order_id,
     *        order_details.line_number,
     *        order_details.description,
     *        order_details.quantity
     *   FROM order_details
     *  WHERE order_details.order_id IN (1, 2);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `fetch eager loading by simple join`(testDB: TestDB) {
        withOrdersTables(testDB) { _, _, _, _, _ ->
            val orderEntities = OrderSchema.Order
                .all()
                .with(OrderSchema.Order::details)
                .toList()

            orderEntities shouldHaveSize 2
            orderEntities.forEach { order ->
                // eager loading 했으므로 fetching 하지 않는다.
                order.details.shouldNotBeEmpty()
            }
        }
    }

    /**
     * Compound Join Conditions
     *
     * ```sql
     * -- Postgres:
     * SELECT orders.id,
     *        orders.order_date,
     *        order_details.line_number,
     *        order_details.description,
     *        order_details.quantity
     *   FROM orders INNER JOIN order_details
     *      ON ((orders.id = order_details.order_id) AND (orders.id = order_details.order_id))
     *  ORDER BY orders.id ASC
     * ```
     * ```sql
     * -- MySQL V8
     * SELECT orders.id,
     *        orders.order_date,
     *        order_details.line_number,
     *        order_details.description,
     *        order_details.quantity
     *   FROM orders INNER JOIN order_details
     *      ON ((orders.id = order_details.order_id) AND (orders.id = order_details.order_id))
     *  ORDER BY orders.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `compound join 01`(testDB: TestDB) {
        withOrdersTables(testDB) { orders, details, _, _, _ ->
            val rows = orders
                .innerJoin(details) {
                    (orders.id eq details.orderId) and (orders.id eq details.orderId)  // 중복된 조건
                }
                .select(
                    orders.id,
                    orders.orderDate,
                    details.lineNumber,
                    details.description,
                    details.quantity
                )
                .orderBy(orders.id)
                .toList()

            rows.forEach { row ->
                log.debug { "orderId=${row[orders.id]}, orderData=${row[orders.orderDate]}, lineNumber=${row[details.lineNumber]}" }
            }
            rows shouldHaveSize 3
        }
    }

    /**
     * 3개의 테이블을 Inner Join 한다.
     *
     * ```sql
     * -- Postgres:
     * SELECT orders.id,
     *        orders.order_date,
     *        order_lines.line_number,
     *        order_lines.item_id,
     *        order_lines.quantity,
     *        items.description
     *   FROM orders
     *      INNER JOIN order_lines ON (order_lines.order_id = orders.id)
     *      INNER JOIN items ON (items.id = order_lines.item_id)
     *  WHERE orders.id = 2;
     *  ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multiple table join`(testDB: TestDB) {
        withOrdersTables(testDB) { orders, _, items, orderLines, _ ->

            val join = orders
                .innerJoin(orderLines) { orderLines.orderId eq orders.id }
                .innerJoin(items) { items.id eq orderLines.itemId }

            val rows = join.select(
                orders.id,
                orders.orderDate,
                orderLines.lineNumber,
                orderLines.itemId,
                orderLines.quantity,
                items.description
            )
                .where { orders.id eq 2L }
                .toList()

            rows.forEach { row ->
                log.debug { "orderId=${row[orders.id]}, lineNumber=${row[orderLines.lineNumber]}, item des=${row[items.description]}" }
            }
            rows shouldHaveSize 2

            with(rows[0]) {
                this[orders.id].value shouldBeEqualTo 2L
                this[orderLines.lineNumber] shouldBeEqualTo 1
                this[orderLines.itemId]?.value shouldBeEqualTo 22L
            }
            with(rows[1]) {
                this[orders.id].value shouldBeEqualTo 2L
                this[orderLines.lineNumber] shouldBeEqualTo 2
                this[orderLines.itemId]?.value shouldBeEqualTo 44L
            }
        }
    }
}
