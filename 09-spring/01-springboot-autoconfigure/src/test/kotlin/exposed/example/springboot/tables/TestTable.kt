package exposed.example.springboot.tables

import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * Exposed의 DDL 작업에 적용되는 테이블
 *
 * ```sql
 * -- H2
 * CREATE TABLE IF NOT EXISTS TEST_TABLE (
 *      ID INT AUTO_INCREMENT PRIMARY KEY,
 *      "name" VARCHAR(100) NOT NULL,
 *      CREATED_AT DATETIME(9) DEFAULT CURRENT_TIMESTAMP NOT NULL
 * )
 * ```
 */
object TestTable: IntIdTable("test_table") {
    val name = varchar("name", 100)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}


class TestEntity(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<TestEntity>(TestTable)

    var name by TestTable.name
    var createdAt by TestTable.createdAt

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = id.value.hashCode()
    override fun toString(): String = toStringBuilder()
        .add("name", name)
        .add("createdAt", createdAt)
        .toString()
}
