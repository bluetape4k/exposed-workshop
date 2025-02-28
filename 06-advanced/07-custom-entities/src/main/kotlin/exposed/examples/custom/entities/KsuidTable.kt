package exposed.examples.custom.entities

import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import io.bluetape4k.idgenerators.ksuid.Ksuid
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
abstract class KsuidEntity(id: KsuidEntityID): StringEntity(id)

abstract class KsuidEntityClass<out E: KsuidEntity>(
    table: KsuidTable,
    entityType: Class<E>? = null,
    entityCtor: ((KsuidEntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
