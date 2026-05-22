package exposed.examples.ktor.observability.repository

import exposed.examples.ktor.observability.model.DiagnosticRecord
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal class ExposedDiagnosticsRepository(
    private val database: Database,
) : DiagnosticsRepository {

    private val initialized = AtomicBoolean(false)
    private val initializeMutex = Mutex()

    override suspend fun ping(): Boolean =
        transactionIO {
            DiagnosticOperations.selectAll().limit(1).toList()
            true
        }

    override suspend fun record(command: RecordOperationCommand): DiagnosticRecord =
        transactionIO {
            val createdAt = Instant.now()
            val id = DiagnosticOperations.insertAndGetId {
                it[name] = command.name
                it[requestId] = command.requestId
                it[durationMs] = command.durationMs
                it[slow] = command.slow
                it[createdAtEpochMs] = createdAt.toEpochMilli()
            }.value
            DiagnosticRecord(
                id = id,
                name = command.name,
                requestId = command.requestId,
                durationMs = command.durationMs,
                slow = command.slow,
                createdAt = createdAt,
            )
        }

    override suspend fun findAll(): List<DiagnosticRecord> =
        transactionIO {
            DiagnosticOperations.selectAll()
                .orderBy(DiagnosticOperations.id to SortOrder.ASC)
                .map(::toRecord)
        }

    private suspend fun <T> transactionIO(block: () -> T): T {
        ensureSchema()
        return withContext(Dispatchers.IO) {
            transaction(database) {
                block()
            }
        }
    }

    private suspend fun ensureSchema() {
        if (initialized.get()) {
            return
        }
        initializeMutex.withLock {
            if (initialized.get()) {
                return
            }
            withContext(Dispatchers.IO) {
                transaction(database) {
                    SchemaUtils.create(DiagnosticOperations)
                }
            }
            initialized.set(true)
        }
    }

    private fun toRecord(row: ResultRow): DiagnosticRecord =
        DiagnosticRecord(
            id = row[DiagnosticOperations.id].value,
            name = row[DiagnosticOperations.name],
            requestId = row[DiagnosticOperations.requestId],
            durationMs = row[DiagnosticOperations.durationMs],
            slow = row[DiagnosticOperations.slow],
            createdAt = Instant.ofEpochMilli(row[DiagnosticOperations.createdAtEpochMs]),
        )
}

private object DiagnosticOperations : LongIdTable("diagnostic_operations") {
    val name = varchar("name", 80)
    val requestId = varchar("request_id", 120)
    val durationMs = long("duration_ms")
    val slow = bool("slow")
    val createdAtEpochMs = long("created_at_epoch_ms")
}
