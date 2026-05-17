package exposed.examples.ktor.architecture

import exposed.examples.ktor.architecture.model.HealthResponse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorArchitectureApplicationTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `index and health endpoints describe the application`() = testApplication {
        application {
            ktorArchitectureModule()
        }

        val index = client.get("/")
        index.status shouldBeEqualTo HttpStatusCode.OK
        index.bodyAsText() shouldContain "ktor-application-architecture"

        val health = json.decodeFromString<HealthResponse>(client.get("/health").bodyAsText())
        health.status shouldBeEqualTo "UP"
    }
}
