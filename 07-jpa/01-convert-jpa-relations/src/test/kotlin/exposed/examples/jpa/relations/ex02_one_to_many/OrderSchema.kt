package exposed.examples.jpa.relations.ex02_one_to_many

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.SizedIterable

object OrderSchema {

    val ordersTables = arrayOf(OrderTable, OrderItemTable)

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS orders (
     *      id SERIAL PRIMARY KEY,
     *      "no" VARCHAR(255) NOT NULL
     * )
     * ```
     */
    object OrderTable: IntIdTable("orders") {
        val no = varchar("no", 255)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS order_items (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      price DECIMAL(10, 2) NULL,
     *      order_id INT NOT NULL,
     *
     *      CONSTRAINT fk_order_items_order_id__id FOREIGN KEY (order_id)
     *      REFERENCES orders(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     *
     * CREATE INDEX order_items_order_id ON order_items (order_id);
     * ```
     */
    object OrderItemTable: IntIdTable("order_items") {
        val name = varchar("name", 255)
        val price = decimal("price", 10, 2).nullable()

        // reference to Order
        val orderId = reference("order_id", OrderTable, onDelete = CASCADE, onUpdate = CASCADE).index()
    }

    class Order(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Order>(OrderTable)

        var no by OrderTable.no

        // one-to-many relationship
        val items: SizedIterable<OrderItem> by OrderItem.referrersOn(OrderItemTable.orderId)

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("no", no)
            .toString()
    }

    class OrderItem(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<OrderItem>(OrderItemTable)

        var name by OrderItemTable.name
        var price by OrderItemTable.price

        // many-to-one relationship
        var order by Order referencedOn OrderItemTable.orderId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("price", price)
            .add("order id", order.id._value)
            .toString()
    }
}
