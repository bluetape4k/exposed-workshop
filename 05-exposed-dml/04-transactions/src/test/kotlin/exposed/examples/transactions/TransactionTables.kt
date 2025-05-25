package exposed.examples.transactions

import exposed.shared.tests.TestDB
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

/**
 * ```sql
 * CREATE TABLE IF NOT EXISTS rollbacktable (
 *      id SERIAL PRIMARY KEY,
 *      "value" VARCHAR(20) NOT NULL
 * );
 * ```
 */
object RollbackTable: IntIdTable("rollbackTable") {
    val value = varchar("value", 20)
}

// Explanation: MariaDB driver never set readonly to true, MSSQL silently ignores the call, SQLite does not
// promise anything, H2 has very limited functionality
val READ_ONLY_EXCLUDED_VENDORS = TestDB.ALL_H2 + TestDB.ALL_MARIADB
// + TestDB.ALL_MARIADB + listOf(TestDB.SQLITE, TestDB.SQLSERVER, TestDB.ORACLE)
