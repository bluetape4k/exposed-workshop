package exposed.shared.tests

import io.bluetape4k.logging.error
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import kotlin.coroutines.CoroutineContext

/**
 * 코루틴 환경에서 지정한 [tables] 를 생성한 뒤 [statement] 를 실행하고 종료 시 정리합니다.
 */
suspend fun withTablesSuspending(
    testDB: TestDB,
    vararg tables: Table,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},
    dropTables: Boolean = true,
    statement: suspend JdbcTransaction.(TestDB) -> Unit,
) {
    withDbSuspending(testDB, context, configure) {
        runCatching {
            SchemaUtils.drop(*tables)
        }

        SchemaUtils.create(*tables)
        try {
            statement(testDB)
            commit()
        } finally {
            if (dropTables) {
                try {
                    SchemaUtils.drop(*tables)
                    commit()
                } catch (ex: Throwable) {
                    logger.error(ex) { "Fail to drop tables, ${tables.joinToString { it.tableName }}" }
                    val database = testDB.db ?: return@withDbSuspending
                    inTopLevelTransaction(
                        db = database,
                        transactionIsolation = database.transactionManager.defaultIsolationLevel
                    ) {
                        maxAttempts = 1
                        SchemaUtils.drop(*tables)
                    }
                }
            }
        }
    }
}

/**
 * [withTablesSuspending] 의 이전 이름입니다.
 */
@Deprecated(
    message = "Use withTablesSuspending() instead.",
    replaceWith =
        ReplaceWith(
            "withTablesSuspending(testDB, *tables, context = context, configure = configure, dropTables = dropTables, statement = statement)",
            "exposed.shared.tests.withTablesSuspending"
        )
)
suspend fun withSuspendedTables(
    testDB: TestDB,
    vararg tables: Table,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = fun DatabaseConfig.Builder.() {
    },
    dropTables: Boolean = true,
    statement: suspend JdbcTransaction.(TestDB) -> Unit,
) {
    withTablesSuspending(
        testDB = testDB,
        tables = tables,
        context = context,
        configure = configure,
        dropTables = dropTables,
        statement = statement
    )
}
