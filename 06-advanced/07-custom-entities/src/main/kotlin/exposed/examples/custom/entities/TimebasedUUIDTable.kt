package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.*

/**
 * 테이블의 `id` 컬럼을 클라이언트에서 생성한 Timebased UUID 값을 사용하는 Table
 */
open class TimebasedUUIDTable(
    name: String = "",
    columnName: String = "id",
): IdTable<UUID>(name) {
    final override val id = uuid(columnName)
        .clientDefault { TimebasedUuid.Reordered.nextId() }
        .entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias TimebasedUUIDEntityID = EntityID<UUID>

/**
 * 엔티티의 `id` 속성을 클라이언트에서 생성한 Timebased UUID 값을 사용하는 Entity
 */
open class TimebasedUUIDEntity(id: TimebasedUUIDEntityID): UUIDEntity(id)

open class TimebasedUUIDEntityClass<out E: TimebasedUUIDEntity>(
    table: TimebasedUUIDTable,
    entityType: Class<E>? = null,
    entityCtor: ((TimebasedUUIDEntityID) -> E)? = null,
): UUIDEntityClass<E>(table, entityType, entityCtor)
