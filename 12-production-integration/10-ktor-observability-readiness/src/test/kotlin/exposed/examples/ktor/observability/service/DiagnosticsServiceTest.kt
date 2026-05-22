package exposed.examples.ktor.observability.service

import exposed.examples.ktor.observability.model.DiagnosticRecord
import exposed.examples.ktor.observability.repository.DiagnosticsRepository
import exposed.examples.ktor.observability.repository.RecordOperationCommand
import io.bluetape4k.assertions.shouldBeEqualTo
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiagnosticsServiceTest {

    @Test
    fun `service marks delayed operations as slow`() = runBlocking {
        val repository = FakeDiagnosticsRepository()
        val service = DiagnosticsService(repository)

        val record = service.runOperation("sync", delayMs = 15, requestId = "service-trace")

        record.slow shouldBeEqualTo true
        repository.commands.single().requestId shouldBeEqualTo "service-trace"
    }

    private class FakeDiagnosticsRepository : DiagnosticsRepository {
        val commands = mutableListOf<RecordOperationCommand>()

        override suspend fun ping(): Boolean =
            true

        override suspend fun record(command: RecordOperationCommand): DiagnosticRecord {
            commands += command
            return DiagnosticRecord(
                id = commands.size.toLong(),
                name = command.name,
                requestId = command.requestId,
                durationMs = command.durationMs,
                slow = command.slow,
                createdAt = Instant.EPOCH,
            )
        }

        override suspend fun findAll(): List<DiagnosticRecord> =
            emptyList()
    }
}
