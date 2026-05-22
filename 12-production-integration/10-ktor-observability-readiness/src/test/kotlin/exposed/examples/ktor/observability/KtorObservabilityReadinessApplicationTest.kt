package exposed.examples.ktor.observability

import exposed.examples.ktor.observability.config.REQUEST_ID_HEADER
import exposed.examples.ktor.observability.model.IndexResponse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
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
class KtorObservabilityReadinessApplicationTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `index endpoint describes observability module`() = testApplication {
        application {
            ktorObservabilityReadinessModule()
        }

        val response = client.get("/") {
            header(REQUEST_ID_HEADER, "ktor-index")
        }

        response.status shouldBeEqualTo HttpStatusCode.OK
        response.headers[REQUEST_ID_HEADER] shouldBeEqualTo "ktor-index"

        val body = response.bodyAsText()
        body shouldContain "ktor-observability-readiness"
        json.decodeFromString<IndexResponse>(body).readiness shouldBeEqualTo "/readyz"
    }
}
