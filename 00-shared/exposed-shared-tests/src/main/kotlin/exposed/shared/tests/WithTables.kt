package exposed.shared.tests

import io.bluetape4k.logging.error
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager

/**
 * 지정한 [tables] 를 생성한 뒤 [statement] 를 실행하고, 종료 시 테이블을 정리합니다.
 *
 * 테스트마다 독립적인 스키마 상태를 보장해야 할 때 사용합니다.
 */
fun withTables(
    testDB: TestDB,
    vararg tables: Table,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},
    dropTables: Boolean = true,
    statement: JdbcTransaction.(TestDB) -> Unit,
) {
    withDb(testDB, configure = configure) {
        runCatching {
            SchemaUtils.drop(*tables)
        }

        SchemaUtils.create(*tables)
        commit()

        try {
            statement(testDB)
            commit() // Need commit to persist data before drop tables
        } finally {
            if (dropTables) {
                try {
                    SchemaUtils.drop(*tables)
                    commit()
                } catch (ex: Exception) {
                    logger.error(ex) { "Drop Tables 에서 예외가 발생했습니다. 삭제할 테이블: ${tables.joinToString { it.tableName }}" }
                    val database = testDB.db ?: return@withDb
                    inTopLevelTransaction(
                        transactionIsolation = database.transactionManager.defaultIsolationLevel,
                        db = database
                    ) {
                        maxAttempts = 1
                        SchemaUtils.drop(*tables)
                    }
                }
            }
        }
    }
}
