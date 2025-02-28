package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.snowflake.Snowflakers
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

/**
 * 테이블의 `id` 컬럼을 클라이언트에서 생성한 Snowflake ID 값을 사용하는 Table
 */
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

/**
 * 엔티티의 `id` 속성을 클라이언트에서 생성한 Snowflake ID 값을 사용하는 Entity
 */
open class SnowflakeEntity(id: SnowflakeEntityID): LongEntity(id)

open class SnowflakeEntityClass<out E: SnowflakeEntity>(
    table: SnowflakeIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((SnowflakeEntityID) -> E)? = null,
): LongEntityClass<E>(table, entityType, entityCtor)
