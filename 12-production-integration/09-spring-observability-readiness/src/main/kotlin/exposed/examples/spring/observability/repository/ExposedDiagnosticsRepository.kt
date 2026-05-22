package exposed.examples.spring.observability.repository

import exposed.examples.spring.observability.model.DiagnosticRecord
import jakarta.annotation.PostConstruct
import java.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository

@Repository
internal class ExposedDiagnosticsRepository(
    private val database: Database,
) : DiagnosticsRepository {

    @PostConstruct
    fun initialize() {
        transaction(database) {
            SchemaUtils.create(DiagnosticOperations)
        }
    }

    override fun ping(): Boolean =
        transaction(database) {
            DiagnosticOperations.selectAll().limit(1).toList()
            true
        }

    override fun record(command: RecordOperationCommand): DiagnosticRecord =
        transaction(database) {
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

    override fun findAll(): List<DiagnosticRecord> =
        transaction(database) {
            DiagnosticOperations.selectAll()
                .orderBy(DiagnosticOperations.id to SortOrder.ASC)
                .map(::toRecord)
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
