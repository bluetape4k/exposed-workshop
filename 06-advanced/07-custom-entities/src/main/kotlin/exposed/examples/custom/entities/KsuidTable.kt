package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.ksuid.Ksuid
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

/**
 * Primary Key 를 Client에서 생성되는 [Ksuid] 값으로 사용하는 Table
 */
open class KsuidTable(name: String = "", columnName: String = "id"): IdTable<String>(name) {
    final override val id: Column<EntityID<String>> =
        varchar(columnName, 27).clientDefault { Ksuid.nextIdAsString() }.entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias KsuidEntityID = EntityID<String>

/**
 * Entity ID 를 [Ksuid]로 생성한 문자열을 사용하는 Entity
 */
open class KsuidEntity(id: KsuidEntityID): Entity<String>(id)

open class KsuidEntityClass<out E: KsuidEntity>(
    table: KsuidTable,
    entityType: Class<E>? = null,
    entityCtor: ((KsuidEntityID) -> E)? = null,
): EntityClass<String, E>(table, entityType, entityCtor)
