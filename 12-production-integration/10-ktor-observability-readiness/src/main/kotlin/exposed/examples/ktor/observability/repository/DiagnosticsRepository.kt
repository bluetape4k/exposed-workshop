package exposed.examples.ktor.observability.repository

import exposed.examples.ktor.observability.model.DiagnosticRecord

internal data class RecordOperationCommand(
    val name: String,
    val requestId: String,
    val durationMs: Long,
    val slow: Boolean,
)

internal interface DiagnosticsRepository {
    suspend fun ping(): Boolean
    suspend fun record(command: RecordOperationCommand): DiagnosticRecord
    suspend fun findAll(): List<DiagnosticRecord>
}
