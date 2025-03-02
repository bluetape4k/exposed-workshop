package exposed.example.springboot.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

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
