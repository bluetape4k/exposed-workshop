package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.*

/**
 * 테이블의 `id` 컬럼을 클라이언트에서 생성한 Timebased UUID 값을 사용하는 Table
 *
 * ### 사용 예제
 *
 * ```kotlin
 * object Orders: TimebasedUUIDTable("orders") {
 *     val userId = varchar("user_id", 50)
 *     val amount = integer("amount")
 * }
 *
 * class OrderEntity(id: TimebasedUUIDEntityID): TimebasedUUIDEntity(id) {
 *     var userId by Orders.userId
 *     var amount by Orders.amount
 *     companion object : TimebasedUUIDEntityClass<OrderEntity>(Orders)
 * }
 *
 * // 코루틴 기반 트랜잭션 예시
 * suspend fun createOrder() = newSuspendedTransaction {
 *     val order = OrderEntity.new {
 *         userId = "user-1"
 *         amount = 5000
 *     }
 *     println(order.id.value) // 생성된 Timebased UUID 값
 * }
 *
 * // batchInsert 예시
 * suspend fun batchInsertOrders() = newSuspendedTransaction {
 *     Orders.batchInsert(listOf("user-1", "user-2")) { uid ->
 *         this[Orders.userId] = uid
 *         this[Orders.amount] = 1000
 *     }
 * }
 * ```
 *
 * @param name 테이블명 (기본값: "")
 * @param columnName PK 컬럼명 (기본값: "id")
 * @author debop
 */
open class TimebasedUUIDTable(
    name: String = "",
    columnName: String = "id",
): IdTable<UUID>(name) {
    /**
     * PK 컬럼. 클라이언트에서 생성한 Timebased UUID 값을 사용한다.
     * 값이 지정되지 않으면 [TimebasedUuid.Reordered.nextId]를 통해 자동 생성된다.
     */
    final override val id = uuid(columnName)
        .clientDefault { TimebasedUuid.Reordered.nextId() }
        .entityId()

    /**
     * PK 설정 (id 컬럼)
     */
    final override val primaryKey = PrimaryKey(id)
}

/**
 * TimebasedUUID 기반 Entity의 ID 타입 별칭
 */
typealias TimebasedUUIDEntityID = EntityID<UUID>

/**
 * 엔티티의 `id` 속성을 클라이언트에서 생성한 Timebased UUID 값을 사용하는 Entity
 *
 * @param id 엔티티의 PK (TimebasedUUIDEntityID)
 */
open class TimebasedUUIDEntity(id: TimebasedUUIDEntityID): UUIDEntity(id)

/**
 * [TimebasedUUIDEntity]를 생성하는 EntityClass
 *
 * @param E 엔티티 타입
 * @param table 매핑되는 테이블
 * @param entityType 엔티티 클래스 타입 (선택)
 * @param entityCtor 엔티티 생성자 (선택)
 */
open class TimebasedUUIDEntityClass<out E: TimebasedUUIDEntity>(
    table: TimebasedUUIDTable,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDEntityID) -> E)? = null,
): UUIDEntityClass<E>(table, entityType, entityCtor)
