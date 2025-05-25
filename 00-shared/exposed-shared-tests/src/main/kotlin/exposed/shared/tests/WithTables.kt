package exposed.shared.tests

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import kotlin.coroutines.CoroutineContext

private val log by lazy { KotlinLogging.logger {} }

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
            commit()
        }

        SchemaUtils.create(*tables)
        commit()

        try {
            statement(testDB)
            commit()               // Need commit to persist data before drop tables
        } finally {
            if (dropTables) {
                try {
                    SchemaUtils.drop(*tables)
                    commit()
                } catch (ex: Exception) {
                    log.error(ex) { "Drop Tables 에서 예외가 발생했습니다. 삭제할 테이블: ${tables.joinToString { it.tableName }}" }
                    val database = testDB.db!!
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

suspend fun withSuspendedTables(
    testDB: TestDB,
    vararg tables: Table,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = { },
    dropTables: Boolean = true,
    statement: suspend JdbcTransaction.(TestDB) -> Unit,
) {
    withSuspendedDb(testDB, context, configure) {
        runCatching {
            SchemaUtils.drop(*tables)
            commit()
        }

        SchemaUtils.create(*tables)
        commit()

        try {
            statement(testDB)
            commit()
        } finally {
            if (dropTables) {
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
}
