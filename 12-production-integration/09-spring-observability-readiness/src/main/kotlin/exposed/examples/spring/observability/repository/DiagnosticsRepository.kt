package exposed.examples.spring.observability.repository

import exposed.examples.spring.observability.model.DiagnosticRecord

internal data class RecordOperationCommand(
    val name: String,
    val requestId: String,
    val durationMs: Long,
    val slow: Boolean,
)

internal interface DiagnosticsRepository {
    fun ping(): Boolean
    fun record(command: RecordOperationCommand): DiagnosticRecord
    fun findAll(): List<DiagnosticRecord>
}
