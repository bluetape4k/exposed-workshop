package exposed.examples.entities

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

/**
 * Exposed DAO 방식의 Entity 사용을 위한 테스트 데이터
 */
object EntityTestData {

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS ytable (
     *      uuid VARCHAR(24) PRIMARY KEY,
     *      x BOOLEAN DEFAULT TRUE NOT NULL
     * )
     */
    object YTable: IdTable<String>("YTable") {
        override val id: Column<EntityID<String>> = varchar("uuid", 24).entityId()
            .clientDefault { EntityID(TimebasedUuid.nextBase62String(), YTable) }

        val x: Column<Boolean> = bool("x").default(true)

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS xtable (
     *      id SERIAL PRIMARY KEY,
     *      b1 BOOLEAN DEFAULT TRUE NOT NULL,
     *      b2 BOOLEAN DEFAULT FALSE NOT NULL,
     *      y1 VARCHAR(24) NULL,
     *
     *      CONSTRAINT fk_xtable_y1__uuid FOREIGN KEY (y1) REFERENCES ytable(uuid)
     *          ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    object XTable: IntIdTable("XTable") {
        val b1: Column<Boolean> = bool("b1").default(true)
        val b2: Column<Boolean> = bool("b2").default(false)
        val y1: Column<EntityID<String>?> = optReference("y1", YTable)
    }

    class XEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<XEntity>(XTable)

        var b1: Boolean by XTable.b1
        var b2: Boolean by XTable.b2

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("b1", b1)
                .add("b2", b2)
                .toString()
    }

    enum class XType {
        A, B
    }

    open class AEntity(id: EntityID<Int>): IntEntity(id) {
        var b1: Boolean by XTable.b1

        companion object: IntEntityClass<AEntity>(XTable) {
            fun create(b1: Boolean, type: XType): AEntity {
                val init: AEntity.() -> Unit = {
                    this.b1 = b1
                }
                val answer = when (type) {
                    XType.B -> BEntity.create { init() }
                    else -> new { init() }
                }
                return answer
            }
        }

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("b1", b1)
                .toString()
    }

    open class BEntity(id: EntityID<Int>): AEntity(id) {
        companion object: IntEntityClass<BEntity>(XTable) {
            fun create(init: AEntity.() -> Unit): BEntity {
                val answer = new { init() }
                return answer
            }
        }

        var b2: Boolean by XTable.b2
        var y: YEntity? by YEntity optionalReferencedOn XTable.y1

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("b1", b1)
                .add("b2", b2)
                .add("y", y)
                .toString()
    }

    class YEntity(id: EntityID<String>): Entity<String>(id) {
        companion object: EntityClass<String, YEntity>(YTable)

        var x: Boolean by YTable.x
        val b: BEntity? by BEntity backReferencedOn XTable.y1

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("x", x)
                .add("b", b)
                .toString()
    }
}
