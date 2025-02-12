package exposed.shared.tests

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.inTopLevelTransaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import kotlin.coroutines.CoroutineContext

private val log = KotlinLogging.logger {}

fun withTables(
    testDB: TestDB,
    vararg tables: Table,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: Transaction.(TestDB) -> Unit,
) {
    withDb(testDB, configure) {
        runCatching {
            SchemaUtils.drop(*tables)
        }

        if (tables.isNotEmpty()) {
            SchemaUtils.create(*tables)
        }
        try {
            statement(testDB)
            commit()  // Need commit to persist data before drop tables
        } finally {
            try {
                if (tables.isNotEmpty()) {
                    SchemaUtils.drop(*tables)
                    commit()
                }
            } catch (ex: Exception) {
                log.error(ex) { "Drop Tables 에서 예외가 발생했습니다. 삭제할 테이블: ${tables.joinToString { it.tableName }}" }
                val database = testDB.db!!
                inTopLevelTransaction(database.transactionManager.defaultIsolationLevel, db = database) {
                    maxAttempts = 1
                    SchemaUtils.drop(*tables)
                }
            }
        }
    }
}

suspend fun withSuspendedTables(
    testDB: TestDB,
    vararg tables: Table,
    context: CoroutineContext? = null,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend Transaction.(TestDB) -> Unit,
) {
    withSuspendedDb(testDB, context, configure) {
        try {
            SchemaUtils.drop(*tables)
        } catch (_: Throwable) {
        }

        SchemaUtils.create(*tables)
        try {
            statement(testDB)
            commit()
        } finally {
            try {
                SchemaUtils.drop(*tables)
                commit()
            } catch (ex: Exception) {
                log.error(ex) { "Fail to drop tables, ${tables.joinToString { it.tableName }}" }
                val database = testDB.db!!
                inTopLevelTransaction(database.transactionManager.defaultIsolationLevel, db = database) {
                    maxAttempts = 1
                    SchemaUtils.drop(*tables)
                }
            }
        }
    }
}
