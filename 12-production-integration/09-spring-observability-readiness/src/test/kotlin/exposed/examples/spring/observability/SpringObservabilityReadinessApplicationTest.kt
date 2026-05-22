package exposed.examples.spring.observability

import exposed.examples.spring.observability.config.REQUEST_ID_HEADER
import exposed.examples.spring.observability.model.ErrorResponse
import exposed.examples.spring.observability.model.OperationDiagnosticsResponse
import exposed.examples.spring.observability.model.OperationsResponse
import exposed.examples.spring.observability.web.DiagnosticsState
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(
    classes = [SpringObservabilityReadinessApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
internal class SpringObservabilityReadinessApplicationTest(
    @param:Autowired private val client: WebTestClient,
    @param:Autowired private val state: DiagnosticsState,
) {

    @BeforeEach
    fun resetState() {
        state.markDatabaseAvailable(true)
    }

    @Test
    fun `actuator readiness reports database success and degraded failure`() {
        client.get()
            .uri("/actuator/health/readiness")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.components.databaseReadiness.status").isEqualTo("UP")

        state.markDatabaseAvailable(false)

        client.get()
            .uri("/actuator/health/readiness")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody()
            .jsonPath("$.status").isEqualTo("DOWN")
            .jsonPath("$.components.databaseReadiness.status").isEqualTo("DOWN")
    }

    @Test
    fun `operation endpoint records slow diagnostics and echoes request id`() {
        val requestId = "spring-trace-1"

        val response = client.get()
            .uri("/diagnostics/operations/import?delayMs=15")
            .header(REQUEST_ID_HEADER, requestId)
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals(REQUEST_ID_HEADER, requestId)
            .expectBody<OperationDiagnosticsResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        response.name shouldBeEqualTo "import"
        response.requestId shouldBeEqualTo requestId
        response.slow shouldBeEqualTo true

        val operations = client.get()
            .uri("/diagnostics/operations")
            .exchange()
            .expectStatus().isOk
            .expectBody<OperationsResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        operations.operations.any { it.id == response.id } shouldBeEqualTo true
    }

    @Test
    fun `invalid request id header is replaced before echo`() {
        val invalidRequestId = "spring trace with spaces"

        val result = client.get()
            .uri("/diagnostics/operations/import")
            .header(REQUEST_ID_HEADER, invalidRequestId)
            .exchange()
            .expectStatus().isOk
            .expectBody<OperationDiagnosticsResponse>()
            .returnResult()

        val response = result.responseBody.shouldNotBeNull()
        val sanitizedRequestId = result.responseHeaders.getFirst(REQUEST_ID_HEADER).shouldNotBeNull()
        sanitizedRequestId shouldNotBeEqualTo invalidRequestId
        response.requestId shouldBeEqualTo sanitizedRequestId
    }

    @Test
    fun `validation errors use structured response with correlation id`() {
        val requestId = "spring-trace-2"

        val response = client.get()
            .uri("/diagnostics/operations/import?delayMs=-1")
            .header(REQUEST_ID_HEADER, requestId)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().valueEquals(REQUEST_ID_HEADER, requestId)
            .expectBody<ErrorResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        response.code shouldBeEqualTo "VALIDATION_FAILED"
        response.requestId shouldBeEqualTo requestId
    }

    @Test
    fun `framework bad requests use structured response with correlation id`() {
        val requestId = "spring-trace-3"

        val response = client.get()
            .uri("/diagnostics/operations/import?delayMs=abc")
            .header(REQUEST_ID_HEADER, requestId)
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().valueEquals(REQUEST_ID_HEADER, requestId)
            .expectBody<ErrorResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        response.code shouldBeEqualTo "BAD_REQUEST"
        response.requestId shouldBeEqualTo requestId
    }

    @Test
    fun `framework method errors preserve http status with structured response`() {
        val requestId = "spring-trace-4"

        val response = client.post()
            .uri("/diagnostics/operations/import")
            .header(REQUEST_ID_HEADER, requestId)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
            .expectHeader().valueEquals(REQUEST_ID_HEADER, requestId)
            .expectBody<ErrorResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        response.code shouldBeEqualTo "HTTP_405"
        response.requestId shouldBeEqualTo requestId
    }
}
