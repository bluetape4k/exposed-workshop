package exposed.examples.ktor.observability.routes

import exposed.examples.ktor.observability.config.REQUEST_ID_HEADER
import exposed.examples.ktor.observability.ktorObservabilityReadinessModule
import exposed.examples.ktor.observability.model.ReadinessResponse
import exposed.examples.ktor.observability.persistence.DiagnosticsPersistence
import exposed.examples.ktor.observability.service.ReadinessState
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.codec.Base58
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadinessRoutesTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `readyz reports success and degraded database state`() = testApplication {
        val state = ReadinessState()
        val persistence = DiagnosticsPersistence.inMemory("observability_ready_${Base58.randomString(8)}")
        application {
            ktorObservabilityReadinessModule(persistence, state)
        }

        val success = client.get("/readyz") {
            header(REQUEST_ID_HEADER, "ready-success")
        }
        success.status shouldBeEqualTo HttpStatusCode.OK
        success.headers[REQUEST_ID_HEADER] shouldBeEqualTo "ready-success"
        json.decodeFromString<ReadinessResponse>(success.bodyAsText()).status shouldBeEqualTo "UP"

        state.markDatabaseAvailable(false)

        val degraded = client.get("/readyz") {
            header(REQUEST_ID_HEADER, "ready-degraded")
        }
        degraded.status shouldBeEqualTo HttpStatusCode.ServiceUnavailable
        degraded.headers[REQUEST_ID_HEADER] shouldBeEqualTo "ready-degraded"
        json.decodeFromString<ReadinessResponse>(degraded.bodyAsText()).status shouldBeEqualTo "DOWN"
    }
}
