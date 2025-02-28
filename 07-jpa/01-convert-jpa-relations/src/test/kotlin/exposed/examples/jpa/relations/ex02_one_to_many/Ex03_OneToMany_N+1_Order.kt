package exposed.examples.jpa.relations.ex02_one_to_many

import exposed.examples.jpa.relations.ex02_one_to_many.OrderSchema.Order
import exposed.examples.jpa.relations.ex02_one_to_many.OrderSchema.OrderItem
import exposed.examples.jpa.relations.ex02_one_to_many.OrderSchema.OrderItemTable
import exposed.examples.jpa.relations.ex02_one_to_many.OrderSchema.OrderTable
import exposed.examples.jpa.relations.ex02_one_to_many.OrderSchema.ordersTables
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

/**
 * bidirectional one-to-many relationship with eager loading and lazy loading
 */
class `Ex03_OneToMany_N+1_Order`: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many bidirectional`(testDB: TestDB) {
        withTables(testDB, *ordersTables) {
            val order1 = createSamples(Random.nextInt(100, 999))
            val items = order1.items.toList()

            flushCache()
            entityCache.clear()

            /**
             * Fetch lazy loading `OrderItem` entity
             * ```sql
             * -- Postgres
             * SELECT orders.id, orders."no"
             *   FROM orders
             *  WHERE orders.id = 1
             * ```
             */
            val loaded = Order.findById(order1.id)!!

            loaded shouldBeEqualTo order1
            // SELECT COUNT(*) FROM order_items WHERE order_items.order_id = 1
            loaded.items.count() shouldBeEqualTo 3

            /**
             * Fetch lazy loading `OrderItem` entity
             *
             * ```sql
             * -- Postgres
             * SELECT order_items.id, order_items."name", order_items.price, order_items.order_id
             *   FROM order_items
             *  WHERE order_items.order_id = 1
             * ```
             */
            loaded.items.toList() shouldBeEqualTo items

            entityCache.clear()

            /**
             * Eager loading `OrderItem` entity by `with(Order::items)`
             *
             * ```sql
             * -- Postgres
             * SELECT orders.id, orders."no" FROM orders;
             *
             * SELECT order_items.id, order_items."name", order_items.price, order_items.order_id
             *   FROM order_items
             *  WHERE order_items.order_id = 1;
             * ```
             */
            val loaded2 = Order.all().with(Order::items).single()
            loaded2 shouldBeEqualTo order1
            loaded2.items.count() shouldBeEqualTo 3
            loaded2.items.toList() shouldBeEqualTo items

            entityCache.clear()

            /**
             * join loading
             * ```sql
             * -- Postgres
             * SELECT orders.id, orders."no", order_items.id, order_items."name", order_items.price, order_items.order_id
             *   FROM orders
             *      INNER JOIN order_items ON orders.id = order_items.order_id
             * ```
             */
            val query: Query = OrderTable.innerJoin(OrderItemTable).selectAll()
            // `wrapRows` 는 Query를 실행해서 엔티티로 빌드합니다.
            val orderItems: List<OrderItem> = OrderItem.wrapRows(query).toList()
            orderItems shouldHaveSize 3
            orderItems.all { it.order == order1 }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many bidirectional delete items`(testDB: TestDB) {
        withTables(testDB, *ordersTables) {
            val order1 = createSamples(Random.nextInt(100, 999))

            flushCache()
            entityCache.clear()

            val item1 = order1.items.first()
            item1.delete()
            OrderItem.all().count() shouldBeEqualTo 2L
            order1.items.count() shouldBeEqualTo 2L

            // cascade delete
            order1.delete()
            OrderItem.all().count() shouldBeEqualTo 0L
        }
    }

    /**
     * Eager loading with pagination
     *
     * ```sql
     * -- Postgres
     * SELECT orders.id, orders."no"
     *   FROM orders
     *  LIMIT 3
     * OFFSET 2;
     *
     * SELECT order_items.id,
     *        order_items."name",
     *        order_items.price,
     *        order_items.order_id
     *   FROM order_items
     *  WHERE order_items.order_id IN (3, 4, 5)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `fetch eager loading with pagination`(testDB: TestDB) {
        withTables(testDB, *ordersTables) {
            val order1 = createSamples(123)

            repeat(10) {
                createSamples(it + 1)
            }

            entityCache.clear()

            val loaded2 = Order.all().offset(2).limit(3).with(Order::items).toList()
            loaded2 shouldHaveSize 3
            loaded2.forEach {
                it.items.toList() shouldHaveSize 3 // 각 order 마다 3개의 item 이 있다.
            }
        }
    }

    /**
     * Lazy loading with pagination (N+1 문제)
     *
     * ```sql
     * -- Postgres
     * SELECT orders.id, orders."no"
     *   FROM orders
     *  LIMIT 3
     * OFFSET 2;
     *
     * SELECT order_items.id, order_items."name", order_items.price, order_items.order_id
     *   FROM order_items
     *  WHERE order_items.order_id = 3;
     * SELECT order_items.id, order_items."name", order_items.price, order_items.order_id
     *   FROM order_items
     *  WHERE order_items.order_id = 4;
     * SELECT order_items.id, order_items."name", order_items.price, order_items.order_id
     *   FROM order_items
     *  WHERE order_items.order_id = 5;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `fetch lazy loading with pagination`(testDB: TestDB) {
        withTables(testDB, *ordersTables) {
            createSamples(123)
            repeat(10) {
                createSamples(it + 1)
            }

            entityCache.clear()

            val loaded2 = Order.all().offset(2).limit(3).toList()

            loaded2 shouldHaveSize 3
            loaded2.forEach {
                it.items.toList() shouldHaveSize 3  // 각 order 마다 3개의 item 이 있다.
            }
        }
    }

    private fun Transaction.createSamples(orderNo: Int): Order {
        val order1 = Order.new { no = "N-$orderNo" }

        OrderItem.new { name = "Item 1 in $orderNo"; order = order1 }
        OrderItem.new { name = "Item 2 in $orderNo"; order = order1 }
        OrderItem.new { name = "Item 3 in $orderNo"; order = order1 }

        commit()
        return order1
    }
}
