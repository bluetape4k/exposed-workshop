package exposed.examples.jpa.ex05_relations.ex02_one_to_many.schema

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.javatime.date

/**
 * one-to-many unidirectional 관계를 Exposed로 구현한 예제
 */
object FamilySchema {

    val familyTables = arrayOf(FatherTable, ChildTable)

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS father (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * )
     * ```
     */
    object FatherTable: IntIdTable("father") {
        val name = varchar("name", 255)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS child (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      birthday DATE NOT NULL,
     *      father_id INT NOT NULL,
     *
     *      CONSTRAINT fk_child_father_id__id FOREIGN KEY (father_id)
     *      REFERENCES father(id) ON DELETE CASCADE ON UPDATE RESTRICT
     * );
     *
     * CREATE INDEX child_father_id ON child (father_id);
     * ```
     */
    object ChildTable: IntIdTable("child") {
        val name = varchar("name", 255)
        val birthday = date("birthday")

        // reference to Father
        val father = reference("father_id", FatherTable, onDelete = ReferenceOption.CASCADE).index()
    }

    class Father(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Father>(FatherTable)

        var name by FatherTable.name

        // Ordered by birthday
        // one-to-many relationship
        val children by Child.referrersOn(ChildTable.father)
            .orderBy(ChildTable.birthday to SortOrder.ASC)

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class Child(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Child>(ChildTable)

        var name by ChildTable.name
        var birthday by ChildTable.birthday

        // many-to-one relationship (bidirectional)
        // var father by Father referencedOn ChildTable.father

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("birthday", birthday)
            .toString()
    }
}
