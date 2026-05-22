package exposed.examples.ktor.observability.routes

import exposed.examples.ktor.observability.config.REQUEST_ID_HEADER
import exposed.examples.ktor.observability.ktorObservabilityReadinessModule
import exposed.examples.ktor.observability.model.ErrorResponse
import exposed.examples.ktor.observability.model.OperationDiagnosticsResponse
import exposed.examples.ktor.observability.model.OperationsResponse
import exposed.examples.ktor.observability.persistence.DiagnosticsPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiagnosticsRoutesTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `operation route records slow diagnostics and echoes request id`() = testApplicationWithPersistence {
        val requestId = "ktor-trace-1"

        val response = client.get("/diagnostics/operations/import?delayMs=15") {
            header(REQUEST_ID_HEADER, requestId)
        }

        response.status shouldBeEqualTo HttpStatusCode.OK
        response.headers[REQUEST_ID_HEADER] shouldBeEqualTo requestId

        val operation = json.decodeFromString<OperationDiagnosticsResponse>(response.bodyAsText())
        operation.name shouldBeEqualTo "import"
        operation.requestId shouldBeEqualTo requestId
        operation.slow shouldBeEqualTo true

        val operations = json.decodeFromString<OperationsResponse>(
            client.get("/diagnostics/operations").bodyAsText()
        )
        operations.operations.any { it.id == operation.id } shouldBeEqualTo true
    }

    @Test
    fun `invalid request id header is replaced before echo`() = testApplicationWithPersistence {
        val invalidRequestId = "ktor trace with spaces"

        val response = client.get("/diagnostics/operations/import") {
            header(REQUEST_ID_HEADER, invalidRequestId)
        }

        response.status shouldBeEqualTo HttpStatusCode.OK
        val sanitizedRequestId = response.headers[REQUEST_ID_HEADER].shouldNotBeNull()
        sanitizedRequestId shouldNotBeEqualTo invalidRequestId

        val operation = json.decodeFromString<OperationDiagnosticsResponse>(response.bodyAsText())
        operation.requestId shouldBeEqualTo sanitizedRequestId
    }

    @Test
    fun `validation errors use structured response with correlation id`() = testApplicationWithPersistence {
        val requestId = "ktor-trace-2"

        val response = client.get("/diagnostics/operations/import?delayMs=-1") {
            header(REQUEST_ID_HEADER, requestId)
        }

        response.status shouldBeEqualTo HttpStatusCode.BadRequest
        response.headers[REQUEST_ID_HEADER] shouldBeEqualTo requestId

        val error = json.decodeFromString<ErrorResponse>(response.bodyAsText())
        error.code shouldBeEqualTo "VALIDATION_FAILED"
        error.requestId shouldBeEqualTo requestId
    }

    private fun testApplicationWithPersistence(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        val persistence = DiagnosticsPersistence.inMemory("observability_routes_${Base58.randomString(8)}")
        application {
            ktorObservabilityReadinessModule(persistence)
        }
        block()
    }
}
