package exposed.shared.tests

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Key
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.nullableTransactionScope
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

private val registeredOnShutdown = mutableSetOf<TestDB>()

internal var currentTestDB by nullableTransactionScope<TestDB>()

object CurrentTestDBInterceptor: StatementInterceptor {
    override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
        return userData.filterValues { it is TestDB }
    }
}

fun withDb(
    testDB: TestDB,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: Transaction.(TestDB) -> Unit,
) {
    logger.info { "Running `withDb` for $testDB" }

    val unregistered = testDB !in registeredOnShutdown
    val newConfiguration = configure != null && !unregistered

    if (unregistered) {
        testDB.beforeConnection()
        Runtimex.addShutdownHook {
            testDB.afterTestFinished()
            registeredOnShutdown.remove(testDB)
        }
        registeredOnShutdown += testDB
        testDB.db = testDB.connect(configure ?: {})
    }

    val registeredDb = testDB.db!!
    try {
        if (newConfiguration) {
            testDB.db = testDB.connect(configure ?: {})
        }
        val database = testDB.db!!
        transaction(database.transactionManager.defaultIsolationLevel, db = database) {
            maxAttempts = 1
            registerInterceptor(CurrentTestDBInterceptor)  // interceptor 를 통해 다양한 작업을 할 수 있다
            currentTestDB = testDB
            statement(testDB)
        }
    } finally {
        // revert any new configuration to not be carried over to the next test in suite
        if (configure != null) {
            testDB.db = registeredDb
        }
    }
}

suspend fun withSuspendedDb(
    testDB: TestDB,
    context: CoroutineContext? = null,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend Transaction.(TestDB) -> Unit,
) {
    logger.info { "Running withSuspendedDb for $testDB" }

    val unregistered = testDB !in registeredOnShutdown
    val newConfiguration = configure != null && !unregistered

    if (unregistered) {
        testDB.beforeConnection()
        Runtimex.addShutdownHook {
            testDB.afterTestFinished()
            registeredOnShutdown.remove(testDB)
        }
        registeredOnShutdown += testDB
        testDB.db = testDB.connect(configure ?: {})
    }

    val registeredDb = testDB.db!!

    try {
        if (newConfiguration) {
            testDB.db = testDB.connect(configure ?: {})
        }
        val database = testDB.db!!

        newSuspendedTransaction(
            context = context,
            db = database,
            transactionIsolation = database.transactionManager.defaultIsolationLevel
        ) {
            maxAttempts = 1
            registerInterceptor(CurrentTestDBInterceptor)
            currentTestDB = testDB
            statement(testDB)
        }
    } finally {
        // revert any new configuration to not be carried over to the next test in suite
        if (configure != null) {
            testDB.db = registeredDb
        }
    }
}
