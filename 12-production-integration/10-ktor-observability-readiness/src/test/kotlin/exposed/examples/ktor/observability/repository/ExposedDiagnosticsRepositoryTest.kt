package exposed.examples.ktor.observability.repository

import exposed.examples.ktor.observability.persistence.DiagnosticsPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedDiagnosticsRepositoryTest {

    @Test
    fun `repository pings and records diagnostics through Exposed JDBC`(): Unit = runBlocking {
        withRepository { repository ->
            repository.ping() shouldBeEqualTo true

            val created = repository.record(
                RecordOperationCommand(
                    name = "sync",
                    requestId = "repo-trace",
                    durationMs = 12,
                    slow = true,
                )
            )

            created.name shouldBeEqualTo "sync"
            repository.findAll() shouldHaveSize 1
            val found = repository.findAll().first()
            found.id shouldBeEqualTo created.id
            found.name shouldBeEqualTo created.name
            found.requestId shouldBeEqualTo created.requestId
            found.durationMs shouldBeEqualTo created.durationMs
            found.slow shouldBeEqualTo created.slow
        }
    }

    private suspend fun withRepository(block: suspend (ExposedDiagnosticsRepository) -> Unit) {
        DiagnosticsPersistence.inMemory("observability_test_${Base58.randomString(8)}").use { persistence ->
            block(ExposedDiagnosticsRepository(persistence.database))
        }
    }
}
