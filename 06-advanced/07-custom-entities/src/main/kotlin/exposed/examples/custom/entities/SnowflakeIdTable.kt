package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.snowflake.Snowflakers
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

open class SnowflakeIdTable(
    name: String = "",
    columnName: String = "id",
): IdTable<Long>(name) {
    final override val id = long(columnName)
        .clientDefault { Snowflakers.Global.nextId() }
        .entityId()

    final override val primaryKey = PrimaryKey(id)
}

typealias SnowflakeEntityID = EntityID<Long>

abstract class SnowflakeEntity(id: SnowflakeEntityID): LongEntity(id)

abstract class SnowflakeEntityClass<out E: SnowflakeEntity>(
    table: SnowflakeIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((SnowflakeEntityID) -> E)? = null,
): LongEntityClass<E>(table, entityType, entityCtor)
