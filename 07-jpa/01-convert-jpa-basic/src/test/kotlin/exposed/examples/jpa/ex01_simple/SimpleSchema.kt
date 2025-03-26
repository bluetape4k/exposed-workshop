package exposed.examples.jpa.ex01_simple

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SizedIterable
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
        val name = varchar("name", 255).uniqueIndex()
        val description = text("description").nullable()
    }

    /**
     * Entity
     */
    class SimpleEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<SimpleEntity>(SimpleTable)

        var name by SimpleTable.name
        var description by SimpleTable.description

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
