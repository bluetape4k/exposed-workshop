package exposed.examples.ktor.httpoutbox

import exposed.examples.ktor.httpoutbox.model.HealthResponse
import exposed.examples.ktor.httpoutbox.persistence.PaymentPersistence
import exposed.examples.ktor.httpoutbox.service.ScenarioPaymentGateway
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.codec.Base58
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorHttpOutboxApplicationTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `index and health endpoints respond`() = testApplication {
        application {
            ktorHttpOutboxModule(
                persistence = PaymentPersistence.inMemory("app_${Base58.randomString(8)}"),
                paymentGateway = ScenarioPaymentGateway()
            )
        }

        val index = client.get("/")
        index.status shouldBeEqualTo HttpStatusCode.OK
        index.bodyAsText() shouldContain "ktor-http-outbox-idempotency"

        val health = json.decodeFromString<HealthResponse>(client.get("/health").bodyAsText())
        health.status shouldBeEqualTo "UP"
    }
}
