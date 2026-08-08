package exposed.examples.ktor.cache

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.util.concurrent.atomic.AtomicBoolean

/** 이 demo module의 implicit JDBC transaction 소유권과 cleanup을 한 곳에서 관리합니다. */
internal class JdbcCacheDatabaseLease private constructor(
    val database: Database,
    private val previousDefault: Database?,
    private val dataSource: HikariDataSource,
    private val token: Any,
) {
    private val released = AtomicBoolean(false)

    fun assertOwned() {
        synchronized(LOCK) {
            check(ownerToken === token && TransactionManager.defaultDatabase === database) {
                "cache demo database is not the active default owner"
            }
        }
    }

    fun release(closeRepository: () -> Unit) {
        if (!released.compareAndSet(false, true)) return
        synchronized(LOCK) {
            var failure: Throwable? = null
            try {
                closeRepository()
            } catch (cause: Throwable) {
                failure = cause
            }
            try {
                if (TransactionManager.defaultDatabase === database) {
                    TransactionManager.defaultDatabase = previousDefault
                }
            } catch (cause: Throwable) {
                failure = failure ?: cause
            }
            try {
                TransactionManager.closeAndUnregister(database)
            } catch (cause: Throwable) {
                failure = failure ?: cause
            }
            try {
                dataSource.close()
            } catch (cause: Throwable) {
                failure = failure ?: cause
            } finally {
                if (ownerToken === token) ownerToken = null
            }
            failure?.let { throw it }
        }
    }

    companion object {
        private val LOCK = Any()
        private var ownerToken: Any? = null

        fun acquire(dataSource: HikariDataSource): JdbcCacheDatabaseLease {
            synchronized(LOCK) {
                check(ownerToken == null) { "cache demo database owner is already active" }
                val previousDefault = TransactionManager.defaultDatabase
                val token = Any()
                var database: Database? = null
                try {
                    database = Database.connect(dataSource)
                    TransactionManager.defaultDatabase = database
                    ownerToken = token
                    return JdbcCacheDatabaseLease(database, previousDefault, dataSource, token)
                } catch (cause: Throwable) {
                    database?.let {
                        if (TransactionManager.defaultDatabase === it) {
                            TransactionManager.defaultDatabase = previousDefault
                        }
                        runCatching { TransactionManager.closeAndUnregister(it) }
                    }
                    runCatching { dataSource.close() }
                    ownerToken = null
                    throw cause
                }
            }
        }
    }
}
