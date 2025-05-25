package exposed.example.springboot.tables.ignored

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * Exposed의 DDL 작업에 적용되지 않는 테이블
 */
object IgnoredTable: IntIdTable("ignored_table") {
    val name = varchar("name", 100)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
