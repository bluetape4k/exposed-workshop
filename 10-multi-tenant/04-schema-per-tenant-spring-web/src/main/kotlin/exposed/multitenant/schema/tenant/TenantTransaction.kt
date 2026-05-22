package exposed.multitenant.schema.tenant

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component
import java.sql.Connection

internal val PUBLIC_SCHEMA = Schema("PUBLIC")

fun interface SchemaResetter {
    fun resetToPublic()
}

class H2SchemaResetter : SchemaResetter {
    override fun resetToPublic() {
        SchemaUtils.setSchema(PUBLIC_SCHEMA)
    }
}

fun interface ConnectionEvictor {
    fun evict(connection: Connection)
}

fun interface ConnectionUsageProbe {
    fun record(operation: String, connection: Connection)
}

fun interface TransactionRollbacker {
    fun rollbackCurrent()
}

class ExposedTransactionRollbacker : TransactionRollbacker {
    override fun rollbackCurrent() {
        TransactionManager.current().rollback()
    }
}

class TenantSchemaResetFailedException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)

@Component
class TenantTransaction(
    private val database: Database,
    private val schemaResetter: SchemaResetter,
    private val transactionRollbacker: TransactionRollbacker,
    private val connectionEvictor: ConnectionEvictor,
    private val connectionUsageProbe: ConnectionUsageProbe,
) {
    companion object : KLogging()

    fun <T> execute(
        tenantId: TenantId = TenantContext.current(),
        operation: String,
        block: () -> T,
    ): T =
        transaction(database) {
            val connection = TransactionManager.current().connection.connection as Connection
            connectionUsageProbe.record(operation, connection)
            var blockFailure: Throwable? = null

            SchemaUtils.setSchema(tenantId.schema)

            try {
                block()
            } catch (e: Throwable) {
                blockFailure = e
                throw e
            } finally {
                try {
                    schemaResetter.resetToPublic()
                } catch (resetFailure: Throwable) {
                    rollbackCurrent(resetFailure)
                    evictConnection(connection, resetFailure)
                    val originalFailure = blockFailure
                    if (originalFailure != null) {
                        originalFailure.addSuppressed(resetFailure)
                    } else {
                        log.warn(resetFailure) {
                            "Failed to reset schema after successful operation. tenant=${tenantId.headerValue}, operation=$operation"
                        }
                        throw TenantSchemaResetFailedException(
                            "Failed to reset tenant schema after $operation",
                            resetFailure
                        )
                    }
                }
            }
        }

    private fun rollbackCurrent(resetFailure: Throwable) {
        try {
            transactionRollbacker.rollbackCurrent()
        } catch (rollbackFailure: Throwable) {
            resetFailure.addSuppressed(rollbackFailure)
        }
    }

    private fun evictConnection(connection: Connection, resetFailure: Throwable) {
        try {
            connectionEvictor.evict(connection)
        } catch (evictFailure: Throwable) {
            resetFailure.addSuppressed(evictFailure)
        }
    }
}
