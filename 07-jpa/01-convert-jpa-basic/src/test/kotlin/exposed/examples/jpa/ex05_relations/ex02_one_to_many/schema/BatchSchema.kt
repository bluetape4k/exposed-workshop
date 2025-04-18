package exposed.examples.jpa.ex05_relations.ex02_one_to_many.schema

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.SizedIterable

/**
 * One-To-Many bidirectional Relationship
 */
object BatchSchema {

    val batchTables = arrayOf(BatchTable, BatchItemTable)

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS batch (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     * ```
     */
    object BatchTable: IntIdTable("batch") {
        val name: Column<String> = varchar("name", 255)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS batch_item (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      batch_id INT NOT NULL,
     *
     *      CONSTRAINT fk_batch_item_batch_id__id FOREIGN KEY (batch_id)
     *      REFERENCES batch(id) ON DELETE CASCADE ON UPDATE RESTRICT
     * );
     * ```
     */
    object BatchItemTable: IntIdTable("batch_item") {
        val name: Column<String> = varchar("name", 255)

        // reference to Batch
        val batchId: Column<EntityID<Int>> = reference("batch_id", BatchTable, onDelete = CASCADE).index()
    }

    class Batch(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Batch>(BatchTable)

        var name: String by BatchTable.name

        // one-to-many relationship
        val items: SizedIterable<BatchItem> by BatchItem referrersOn BatchItemTable.batchId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class BatchItem(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<BatchItem>(BatchItemTable)

        var name: String by BatchItemTable.name

        // many-to-one relationship
        var batch by Batch referencedOn BatchItemTable.batchId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("batch id", batch.id._value)
            .toString()
    }
}
