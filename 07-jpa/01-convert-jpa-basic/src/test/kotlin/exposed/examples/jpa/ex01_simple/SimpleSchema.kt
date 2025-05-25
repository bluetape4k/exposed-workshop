package exposed.examples.jpa.ex01_simple

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.support.requireNotBlank
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.io.Serializable

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
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("description", description)
            .toString()
    }

    /**
     * DTO
     */
    data class SimpleDTO(
        val id: Long,
        val name: String,
        val description: String?,
    ): Serializable

    fun SimpleEntity.toSimpleDTO(): SimpleDTO {
        return SimpleDTO(
            id = this.id.value,
            name = this.name,
            description = this.description
        )
    }

    fun ResultRow.toSimpleDTO(): SimpleDTO {
        return SimpleDTO(
            id = this[SimpleTable.id].value,
            name = this[SimpleTable.name],
            description = this[SimpleTable.description]
        )
    }

    fun SizedIterable<ResultRow>.toSimpleDTOs(): List<SimpleDTO> {
        return this.map { it.toSimpleDTO() }
    }
}
