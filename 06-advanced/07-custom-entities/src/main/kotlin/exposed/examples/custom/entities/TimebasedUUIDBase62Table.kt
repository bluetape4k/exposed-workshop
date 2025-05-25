package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

/**
 * 테이블의 `id` 컬럼을 클라이언트에서 생성한 Timebased UUID 값을 Base62로 인코딩한 값을 사용하는 Table
 */
open class TimebasedUUIDBase62Table(
    name: String = "",
    columnName: String = "id",
): IdTable<String>(name) {
    final override val id = varchar(columnName, 22)
        .clientDefault { TimebasedUuid.Reordered.nextIdAsString() }
        .entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias TimebasedUUIDBase62EntityID = EntityID<String>

/**
 * 엔티티의 `id` 속성을 클라이언트에서 생성한 Timebased UUID 값을 사용하는 Entity
 */
open class TimebasedUUIDBase62Entity(id: TimebasedUUIDBase62EntityID): Entity<String>(id)

open class TimebasedUUIDBase62EntityClass<out E: TimebasedUUIDBase62Entity>(
    table: TimebasedUUIDBase62Table,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDBase62EntityID) -> E)? = null,
): EntityClass<String, E>(table, entityType, entityCtor)
