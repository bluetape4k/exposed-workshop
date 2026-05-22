package exposed.examples.ktor.observability.service

import exposed.examples.ktor.observability.model.DiagnosticRecord
import exposed.examples.ktor.observability.repository.DiagnosticsRepository
import exposed.examples.ktor.observability.repository.RecordOperationCommand
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime
import kotlinx.coroutines.delay

private const val SLOW_OPERATION_THRESHOLD_MS = 10L

internal class DiagnosticsService(
    private val repository: DiagnosticsRepository,
) {

    suspend fun runOperation(name: String, delayMs: Long, requestId: String): DiagnosticRecord {
        require(name.isNotBlank()) { "operation name must not be blank" }
        require(delayMs >= 0) { "delayMs must be greater than or equal to zero" }

        val durationNanos = measureNanoTime {
            if (delayMs > 0) {
                delay(delayMs)
            }
        }
        val durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos)
        return repository.record(
            RecordOperationCommand(
                name = name,
                requestId = requestId,
                durationMs = durationMs,
                slow = durationMs >= SLOW_OPERATION_THRESHOLD_MS,
            )
        )
    }

    suspend fun findOperations(): List<DiagnosticRecord> =
        repository.findAll()
}
