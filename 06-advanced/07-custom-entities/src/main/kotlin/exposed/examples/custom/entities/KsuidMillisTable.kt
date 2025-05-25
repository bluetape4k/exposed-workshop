package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.ksuid.KsuidMillis
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

/**
 * Primary Key 를 Client에서 생성되는 [KsuidMillis] 값으로 사용하는 Table
 */
open class KsuidMillisTable(name: String = "", columnName: String = "id"): IdTable<String>(name) {
    final override val id: Column<EntityID<String>> =
        varchar(columnName, 27).clientDefault { KsuidMillis.nextIdAsString() }.entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias KsuidMillisEntityID = EntityID<String>

/**
 * Entity ID 를 [KsuidMillis]로 생성한 문자열을 사용하는 Entity
 */
open class KsuidMillisEntity(id: KsuidEntityID): Entity<String>(id)

open class KsuidMillisEntityClass<out E: KsuidMillisEntity>(
    table: KsuidMillisTable,
    entityType: Class<E>? = null,
    entityCtor: ((KsuidMillisEntityID) -> E)? = null,
): EntityClass<String, E>(table, entityType, entityCtor)
