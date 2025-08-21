package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

/**
 * 테이블의 `id` 컬럼을 클라이언트에서 생성한 Timebased UUID 값을 Base62로 인코딩한 값을 사용하는 Table
 *
 * ### 사용 예제
 *
 * ```kotlin
 * object Events: TimebasedUUIDBase62Table("events") {
 *     val title = varchar("title", 100)
 *     val timestamp = long("timestamp")
 * }
 *
 * class EventEntity(id: TimebasedUUIDBase62EntityID): TimebasedUUIDBase62Entity(id) {
 *     var title by Events.title
 *     var timestamp by Events.timestamp
 *     companion object : TimebasedUUIDBase62EntityClass<EventEntity>(Events)
 * }
 *
 * // 코루틴 기반 트랜잭션 예시
 * suspend fun createEvent() = newSuspendedTransaction {
 *     val event = EventEntity.new {
 *         title = "세미나"
 *         timestamp = System.currentTimeMillis()
 *     }
 *     println(event.id.value) // 생성된 Base62 인코딩 UUID 값
 * }
 *
 * // batchInsert 예시
 * suspend fun batchInsertEvents() = newSuspendedTransaction {
 *     Events.batchInsert(listOf("A", "B")) { t ->
 *         this[Events.title] = t
 *         this[Events.timestamp] = System.currentTimeMillis()
 *     }
 * }
 * ```
 *
 * @param name 테이블명 (기본값: "")
 * @param columnName PK 컬럼명 (기본값: "id")
 * @author debop
 */
open class TimebasedUUIDBase62Table(
    name: String = "",
    columnName: String = "id",
): IdTable<String>(name) {
    /**
     * PK 컬럼. 클라이언트에서 생성한 Timebased UUID 값을 Base62로 인코딩하여 22자 문자열로 저장한다.
     * 값이 지정되지 않으면 [TimebasedUuid.Reordered.nextIdAsString]을 통해 자동 생성된다.
     */
    final override val id = varchar(columnName, 22)
        .clientDefault { TimebasedUuid.Reordered.nextIdAsString() }
        .entityId()

    /**
     * PK 설정 (id 컬럼)
     */
    final override val primaryKey = PrimaryKey(id)
}

/**
 * TimebasedUUID(Base62) 기반 Entity의 ID 타입 별칭
 */
typealias TimebasedUUIDBase62EntityID = EntityID<String>

/**
 * 엔티티의 `id` 속성을 클라이언트에서 생성한 Timebased UUID(Base62) 값을 사용하는 Entity
 *
 * @param id 엔티티의 PK (TimebasedUUIDBase62EntityID)
 */
open class TimebasedUUIDBase62Entity(id: TimebasedUUIDBase62EntityID): Entity<String>(id)

/**
 * [TimebasedUUIDBase62Entity]를 생성하는 EntityClass
 *
 * @param E 엔티티 타입
 * @param table 매핑되는 테이블
 * @param entityType 엔티티 클래스 타입 (선택)
 * @param entityCtor 엔티티 생성자 (선택)
 */
open class TimebasedUUIDBase62EntityClass<out E: TimebasedUUIDBase62Entity>(
    table: TimebasedUUIDBase62Table,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDBase62EntityID) -> E)? = null,
): EntityClass<String, E>(table, entityType, entityCtor)
