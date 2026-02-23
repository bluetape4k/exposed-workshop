package exposed.examples.jpa.ex01_simple

import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.support.requireNotBlank
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.io.Serializable

/**
 * 단순 엔티티 매핑 예제에서 사용하는 테이블/엔티티/DTO 변환 함수를 제공합니다.
 */
object SimpleSchema {

    /**
     * Table
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS simple_entity (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      description TEXT NULL
     * );
     *
     * ALTER TABLE simple_entity
     *      ADD CONSTRAINT simple_entity_name_unique UNIQUE ("name");
     * ```
     */
    object SimpleTable: LongIdTable("simple_entity") {
        val name: Column<String> = varchar("name", 255).uniqueIndex()
        val description: Column<String?> = text("description").nullable()
    }

    /**
     * Entity
     */
    class SimpleEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<SimpleEntity>(SimpleTable) {
            /**
             * 이름으로 `SimpleEntity`를 생성합니다.
             *
             * @throws IllegalArgumentException 이름이 비어있거나 공백인 경우
             */
            fun new(name: String): SimpleEntity {
                name.requireNotBlank("name")
                return SimpleEntity.new {
                    this.name = name
                }
            }
        }

        var name: String by SimpleTable.name
        var description: String? by SimpleTable.description

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("name", name)
            .add("description", description)
            .toString()
    }

    /**
     * Record
     */
    data class SimpleRecord(
        val id: Long,
        val name: String,
        val description: String?,
    ): Serializable {
        /**
         * 식별자만 변경한 새 레코드를 반환합니다.
         */
        fun withId(id: Long) = copy(id = id)
    }

    /**
     * DAO 엔티티를 `SimpleRecord`로 변환합니다.
     */
    fun SimpleEntity.toSimpleRecord(): SimpleRecord {
        return SimpleRecord(
            id = this.id.value,
            name = this.name,
            description = this.description
        )
    }

    /**
     * 조회 결과 행을 `SimpleRecord`로 변환합니다.
     */
    fun ResultRow.toSimpleRecord(): SimpleRecord {
        return SimpleRecord(
            id = this[SimpleTable.id].value,
            name = this[SimpleTable.name],
            description = this[SimpleTable.description]
        )
    }

    /**
     * 조회 결과 집합을 `SimpleRecord` 목록으로 변환합니다.
     */
    fun SizedIterable<ResultRow>.toSimpleRecords(): List<SimpleRecord> {
        return this.map { it.toSimpleRecord() }
    }
}
