package exposed.shared.mapping

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.io.Serializable
import java.time.LocalDate

/**
 * 주문(Order), 주문 상세(OrderDetail), 상품(Item), 주문 라인(OrderLine), 사용자(User) 엔티티와 테이블을 포함하는 스키마 정의 객체.
 *
 * DAO 패턴을 사용하여 주문 처리 관련 엔티티 간의 관계를 나타냅니다.
 */
object OrderSchema {

    val allOrderTables = arrayOf(OrderTable, OrderDetailTable, ItemTable, OrderLineTable, UserTable)

    object OrderTable: LongIdTable("orders") {
        val orderDate = date("order_date")
    }

    object OrderDetailTable: LongIdTable("order_details") {
        val orderId = reference("order_id", OrderTable)
        val lineNumber = integer("line_number")
        val description = varchar("description", 255)
        val quantity = integer("quantity")
    }

    object ItemTable: LongIdTable("items") {
        val description = varchar("description", 255)
    }

    object OrderLineTable: LongIdTable("order_lines") {
        val orderId = reference("order_id", OrderTable)
        val itemId = optReference("item_id", ItemTable)
        val lineNumber = integer("line_number")
        val quantity = integer("quantity")
    }

    object UserTable: LongIdTable("users") {
        val userName = varchar("user_name", 255)
        val parentId = reference("parent_id", UserTable).nullable()
    }

    /** 주문 엔티티. [OrderTable]과 매핑됩니다. */
    class Order(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<Order>(OrderTable)

        val details by OrderDetail referrersOn OrderDetailTable.orderId
        var orderDate by OrderTable.orderDate

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("orderDate", orderDate)
            .toString()
    }

    /** 주문 상세 엔티티. [OrderDetailTable]과 매핑됩니다. */
    class OrderDetail(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<OrderDetail>(OrderDetailTable)

        var order by Order referencedOn OrderDetailTable
        var lineNumber by OrderDetailTable.lineNumber
        var description by OrderDetailTable.description
        var quantity by OrderDetailTable.quantity

        val orderId by OrderDetailTable.orderId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("orderId", orderId)
            .add("lineNumber", lineNumber)
            .add("description", description)
            .add("quantity", quantity)
            .toString()
    }

    /** 상품 엔티티. [ItemTable]과 매핑됩니다. */
    class Item(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<Item>(ItemTable)

        var description by ItemTable.description

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("description", description)
            .toString()
    }

    /** 주문 라인 엔티티. [OrderLineTable]과 매핑됩니다. */
    class OrderLine(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<OrderLine>(OrderLineTable)

        var order by Order referencedOn OrderLineTable
        var item by Item optionalReferencedOn OrderLineTable
        var lineNumber by OrderLineTable.lineNumber
        var quantity by OrderLineTable.quantity

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("orderId", order.id.value)
            .add("itemId", item?.id?.value)
            .add("lineNumber", lineNumber)
            .add("quantity", quantity)
            .toString()
    }

    /** 사용자 엔티티. [UserTable]과 매핑됩니다. */
    class User(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<User>(UserTable)

        var userName by UserTable.userName
        var parent by User optionalReferencedOn UserTable.parentId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("userName", userName)
            .add("parentId", parent?.id?._value)
            .toString()
    }

    /**
     * 주문 조회 결과를 담는 데이터 클래스.
     *
     * @property orderId 주문 ID
     * @property itemId 상품 ID
     * @property quantity 수량
     * @property description 설명
     */
    data class OrderRecord(
        val orderId: Long? = null,
        val itemId: Long? = null,
        val quantity: Int? = null,
        val description: String? = null,
    ): Comparable<OrderRecord>, Serializable {
        override fun compareTo(other: OrderRecord): Int =
            orderId?.compareTo(other.orderId ?: 0)
                ?: itemId?.compareTo(other.itemId ?: 0)
                ?: 0
    }

    /**
     * 주문 관련 테이블을 생성하고 샘플 데이터를 삽입한 후 [statement]를 실행합니다.
     *
     * @param testDB 사용할 테스트 데이터베이스
     * @param statement 테이블과 샘플 데이터가 준비된 상태에서 실행할 트랜잭션 블록
     */
    @Suppress("UnusedReceiverParameter")
    fun AbstractExposedTest.withOrdersTables(
        testDB: TestDB,
        statement: JdbcTransaction.(
            orders: OrderTable,
            orderDetails: OrderDetailTable,
            items: ItemTable,
            orderLines: OrderLineTable,
            users: UserTable,
        ) -> Unit,
    ) {

        val orders = OrderTable
        val orderDetails = OrderDetailTable
        val items = ItemTable
        val orderLines = OrderLineTable
        val users = UserTable

        withTables(testDB, *OrderSchema.allOrderTables) {
            val order1 = Order.new {
                orderDate = LocalDate.of(2017, 1, 17)
            }
            OrderDetail.new {
                order = order1
                lineNumber = 1
                description = "Tennis Ball"
                quantity = 3
            }
            OrderDetail.new {
                order = order1
                lineNumber = 2
                description = "Tennis Racket"
                quantity = 1
            }

            val order2 = Order.new {
                orderDate = LocalDate.of(2017, 1, 18)
            }
            OrderDetail.new {
                order = order2
                lineNumber = 1
                description = "Football"
                quantity = 2
            }

            val item1 = Item.new(22) {
                description = "Helmet"
            }
            val item2 = Item.new(33) {
                description = "First Base Glove"
            }
            val item3 = Item.new(44) {
                description = "Outfield Glove"
            }
            Item.new(55) {
                description = "Catcher Glove"
            }

            OrderLine.new {
                order = order1
                item = item1
                lineNumber = 1
                quantity = 1
            }
            OrderLine.new {
                order = order1
                item = item2
                lineNumber = 2
                quantity = 1
            }
            OrderLine.new {
                order = order2
                item = item1
                lineNumber = 1
                quantity = 1
            }
            OrderLine.new {
                order = order2
                item = item3
                lineNumber = 2
                quantity = 1
            }
            OrderLine.new {
                order = order2
                item = null
                lineNumber = 3
                quantity = 6
            }

            val fred = User.new {
                userName = "Fred"
            }
            val barney = User.new {
                userName = "Barney"
            }
            User.new {
                userName = "Pebbles"
                parent = fred
            }
            User.new {
                userName = "Bamm Bamm"
                parent = barney
            }

            flushCache()
            entityCache.clear()

            statement(orders, orderDetails, items, orderLines, users)
        }
    }
}
