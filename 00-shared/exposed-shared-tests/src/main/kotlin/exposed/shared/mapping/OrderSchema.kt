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
 * 주문(Order) 도메인의 테이블 정의와 DAO 엔티티, 데이터 클래스를 포함하는 스키마 객체.
 *
 * 이 스키마는 주문, 주문 상세, 아이템, 주문 라인, 사용자 테이블로 구성되며
 * 매핑 관련 테스트에서 샘플 데이터와 함께 사용됩니다.
 *
 * 포함된 테이블:
 * - [OrderTable]: 주문 정보
 * - [OrderDetailTable]: 주문 상세 정보
 * - [ItemTable]: 아이템 정보
 * - [OrderLineTable]: 주문 라인 정보
 * - [UserTable]: 사용자 정보 (자기 참조 계층 구조)
 */
object OrderSchema {

    /** 모든 주문 관련 테이블의 배열. 테이블 생성/삭제 시 사용됩니다. */
    val allOrderTables = arrayOf(OrderTable, OrderDetailTable, ItemTable, OrderLineTable, UserTable)

    /**
     * 주문 정보를 저장하는 테이블.
     *
     * - `id`: 자동 생성되는 Long 타입 기본 키
     * - `order_date`: 주문 날짜
     */
    object OrderTable: LongIdTable("orders") {
        val orderDate = date("order_date")
    }

    /**
     * 주문 상세 정보를 저장하는 테이블.
     *
     * - `order_id`: [OrderTable]을 참조하는 외래 키
     * - `line_number`: 주문 내 라인 번호
     * - `description`: 상품 설명
     * - `quantity`: 수량
     */
    object OrderDetailTable: LongIdTable("order_details") {
        val orderId = reference("order_id", OrderTable)
        val lineNumber = integer("line_number")
        val description = varchar("description", 255)
        val quantity = integer("quantity")
    }

    /**
     * 아이템(상품) 정보를 저장하는 테이블.
     *
     * - `description`: 아이템 설명
     */
    object ItemTable: LongIdTable("items") {
        val description = varchar("description", 255)
    }

    /**
     * 주문과 아이템 간의 연결 정보를 저장하는 주문 라인 테이블.
     *
     * - `order_id`: [OrderTable]을 참조하는 외래 키
     * - `item_id`: [ItemTable]을 참조하는 선택적 외래 키 (NULL 허용)
     * - `line_number`: 주문 내 라인 번호
     * - `quantity`: 수량
     */
    object OrderLineTable: LongIdTable("order_lines") {
        val orderId = reference("order_id", OrderTable)
        val itemId = optReference("item_id", ItemTable)
        val lineNumber = integer("line_number")
        val quantity = integer("quantity")
    }

    /**
     * 사용자 정보를 저장하는 테이블.
     *
     * 자기 참조(self-reference)를 통해 계층적 사용자 구조를 표현합니다.
     *
     * - `user_name`: 사용자 이름
     * - `parent_id`: 부모 사용자를 참조하는 선택적 외래 키 (NULL 허용)
     */
    object UserTable: LongIdTable("users") {
        val userName = varchar("user_name", 255)
        val parentId = reference("parent_id", UserTable).nullable()
    }

    /**
     * 주문(Order) DAO 엔티티 클래스.
     *
     * @param id 엔티티 식별자
     * @property details 이 주문에 속한 [OrderDetail] 목록
     * @property orderDate 주문 날짜
     */
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

    /**
     * 주문 상세(OrderDetail) DAO 엔티티 클래스.
     *
     * @param id 엔티티 식별자
     * @property order 연결된 [Order] 엔티티
     * @property lineNumber 주문 내 라인 번호
     * @property description 상품 설명
     * @property quantity 수량
     * @property orderId 주문 ID (읽기 전용)
     */
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

    /**
     * 아이템(Item) DAO 엔티티 클래스.
     *
     * @param id 엔티티 식별자
     * @property description 아이템 설명
     */
    class Item(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<Item>(ItemTable)

        var description by ItemTable.description

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("description", description)
            .toString()
    }

    /**
     * 주문 라인(OrderLine) DAO 엔티티 클래스.
     *
     * @param id 엔티티 식별자
     * @property order 연결된 [Order] 엔티티
     * @property item 연결된 [Item] 엔티티 (선택적, NULL 가능)
     * @property lineNumber 주문 내 라인 번호
     * @property quantity 수량
     */
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

    /**
     * 사용자(User) DAO 엔티티 클래스.
     *
     * 계층적 사용자 구조를 지원하며, `parent` 속성으로 부모 사용자를 참조합니다.
     *
     * @param id 엔티티 식별자
     * @property userName 사용자 이름
     * @property parent 부모 사용자 엔티티 (선택적, NULL 가능)
     */
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
     * 주문 ID, 아이템 ID, 수량, 설명을 포함하며
     * 정렬 시 orderId, itemId 순서로 비교합니다.
     *
     * @property orderId 주문 ID (선택적)
     * @property itemId 아이템 ID (선택적)
     * @property quantity 수량 (선택적)
     * @property description 상품 설명 (선택적)
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
     * 주문 관련 테이블을 생성하고 샘플 데이터를 삽입한 후 테스트 블록을 실행합니다.
     *
     * 테스트 완료 후 모든 테이블을 자동으로 삭제합니다.
     * 샘플 데이터로 주문 2건, 아이템 4개, 주문 라인 5개, 사용자 4명이 생성됩니다.
     *
     * @param testDB 테스트에 사용할 데이터베이스 종류
     * @param statement 테이블 참조와 함께 실행할 트랜잭션 코드 블록
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
