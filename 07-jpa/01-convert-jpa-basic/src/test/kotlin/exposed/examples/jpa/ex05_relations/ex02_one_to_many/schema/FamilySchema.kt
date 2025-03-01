package exposed.examples.jpa.ex05_relations.ex02_one_to_many.schema

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.javatime.date

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
        val father = reference("father_id", FatherTable, onDelete = CASCADE).index()
    }

    class Father(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Father>(FatherTable)

        var name by FatherTable.name

        // Ordered by birthday
        // one-to-many relationship
        val children by Child.referrersOn(ChildTable.father)
            .orderBy(ChildTable.birthday to ASC)

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
