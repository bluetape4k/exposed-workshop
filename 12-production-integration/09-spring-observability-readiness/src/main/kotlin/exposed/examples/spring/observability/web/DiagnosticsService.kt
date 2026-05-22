package exposed.examples.spring.observability.web

import exposed.examples.spring.observability.model.DiagnosticRecord
import exposed.examples.spring.observability.repository.DiagnosticsRepository
import exposed.examples.spring.observability.repository.RecordOperationCommand
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureNanoTime
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

private const val SLOW_OPERATION_THRESHOLD_MS = 10L

@Component
internal class DiagnosticsState {
    private val databaseAvailable = AtomicBoolean(true)

    fun markDatabaseAvailable(available: Boolean) {
        databaseAvailable.set(available)
    }

    fun isDatabaseAvailable(): Boolean =
        databaseAvailable.get()
}

@Service
internal class DiagnosticsService(
    private val repository: DiagnosticsRepository,
) {

    fun runOperation(name: String, delayMs: Long, requestId: String): DiagnosticRecord {
        require(name.isNotBlank()) { "operation name must not be blank" }
        require(delayMs >= 0) { "delayMs must be greater than or equal to zero" }

        val durationNanos = measureNanoTime {
            if (delayMs > 0) {
                sleep(delayMs)
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

    fun findOperations(): List<DiagnosticRecord> =
        repository.findAll()

    private fun sleep(delayMs: Long) {
        try {
            TimeUnit.MILLISECONDS.sleep(delayMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("operation interrupted", e)
        }
    }
}
