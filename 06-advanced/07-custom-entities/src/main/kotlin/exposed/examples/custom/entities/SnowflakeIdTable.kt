package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.snowflake.Snowflakers
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * 테이블의 `id` 컬럼에 클라이언트에서 생성한 Snowflake ID 값을 사용하는 [IdTable]입니다.
 *
 * @property name 테이블명
 * @property columnName ID 컬럼명 (기본값: "id")
 * @author debop
 */
open class SnowflakeIdTable(
    name: String = "",
    columnName: String = "id",
): IdTable<Long>(name) {
    /**
     * Snowflake ID 값을 저장하는 `id` 컬럼입니다.
     * 클라이언트에서 값을 지정하지 않으면, [Snowflakers.Global]을 통해 새로운 ID를 발급받아 설정합니다.
     */
    final override val id = long(columnName)
        .clientDefault { Snowflakers.Global.nextId() }
        .entityId()

    /**
     * Primary key 설정 (기본키는 `id` 컬럼입니다).
     */
    final override val primaryKey = PrimaryKey(id)
}

/**
 * Snowflake ID를 사용하는 엔티티의 ID 타입을 나타내는 별칭입니다. [EntityID]의 Long 타입입니다.
 */
typealias SnowflakeEntityID = EntityID<Long>

/**
 * 엔티티의 `id` 속성에 클라이언트에서 생성한 Snowflake ID 값을 사용하는 [LongEntity]입니다.
 *
 * @property id 엔티티의 ID
 */
open class SnowflakeIdEntity(id: SnowflakeEntityID): LongEntity(id)

/**
 * [SnowflakeIdEntity]를 생성하는 [LongEntityClass]입니다.
 *
 * @param E 엔티티의 타입
 * @property table 엔티티와 매핑된 [SnowflakeIdTable]
 * @property entityType 엔티티의 클래스 타입 (선택 사항)
 * @property entityCtor 엔티티 생성자 (선택 사항)
 */
open class SnowflakeEntityClass<out E: SnowflakeIdEntity>(
    table: SnowflakeIdTable,
    entityType: Class<E>? = null,
    entityCtor: ((SnowflakeEntityID) -> E)? = null,
): LongEntityClass<E>(table, entityType, entityCtor)
