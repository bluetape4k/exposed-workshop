package exposed.examples.ktor.httpoutbox.routes

import exposed.examples.ktor.httpoutbox.ktorHttpOutboxModule
import exposed.examples.ktor.httpoutbox.model.CreatePaymentRequest
import exposed.examples.ktor.httpoutbox.model.ErrorResponse
import exposed.examples.ktor.httpoutbox.model.PaymentResponse
import exposed.examples.ktor.httpoutbox.model.PaymentsResponse
import exposed.examples.ktor.httpoutbox.persistence.PaymentPersistence
import exposed.examples.ktor.httpoutbox.service.ScenarioPaymentGateway
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentRoutesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `payment routes cover success duplicate permanent and retry paths`() = testApplicationWithPersistence { gateway ->
        gateway.succeed("ok-key", "ext-ok")
        val created = createPayment(CreatePaymentRequest(" order-ok ", 2_500, " ok-key "))
        created.status.name shouldBeEqualTo "SUCCEEDED"
        created.externalId shouldBeEqualTo "ext-ok"

        val duplicateResponse = postJson("/payments", CreatePaymentRequest("order-ok", 2_500, "ok-key"))
        duplicateResponse.status shouldBeEqualTo HttpStatusCode.OK
        json.decodeFromString<PaymentResponse>(duplicateResponse.bodyAsText()).duplicate shouldBeEqualTo true

        gateway.retryableThenSuccess("retry-key", "ext-retry")
        val retryable = createPayment(CreatePaymentRequest("order-retry", 1_000, "retry-key"))
        retryable.status.name shouldBeEqualTo "RETRYABLE_FAILED"

        val retried = json.decodeFromString<PaymentResponse>(
            client.post("/payments/${retryable.id}/retry").bodyAsText()
        )
        retried.status.name shouldBeEqualTo "SUCCEEDED"
        retried.attempts shouldBeEqualTo 2

        gateway.permanent("bad-key")
        val permanent = createPayment(CreatePaymentRequest("order-bad", 1_000, "bad-key"))
        permanent.status.name shouldBeEqualTo "PERMANENT_FAILED"

        val list = json.decodeFromString<PaymentsResponse>(client.get("/payments").bodyAsText())
        list.payments shouldHaveSize 3
    }

    @Test
    fun `validation and not found errors are sanitized`() = testApplicationWithPersistence { _ ->
        val validation = postJson("/payments", CreatePaymentRequest("", 1_000, "key"))
        validation.status shouldBeEqualTo HttpStatusCode.BadRequest
        json.decodeFromString<ErrorResponse>(validation.bodyAsText()).code shouldBeEqualTo "VALIDATION_ERROR"

        val notFound = client.get("/payments/999")
        notFound.status shouldBeEqualTo HttpStatusCode.NotFound
        json.decodeFromString<ErrorResponse>(notFound.bodyAsText()).code shouldBeEqualTo "NOT_FOUND"
    }

    private suspend fun ApplicationTestBuilder.createPayment(request: CreatePaymentRequest): PaymentResponse {
        val response = postJson("/payments", request)
        response.status shouldBeEqualTo HttpStatusCode.Created
        return json.decodeFromString(response.bodyAsText())
    }

    private suspend inline fun <reified T> ApplicationTestBuilder.postJson(
        path: String,
        body: T,
    ) = client.post(path) {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(body))
    }

    private fun testApplicationWithPersistence(
        test: suspend ApplicationTestBuilder.(ScenarioPaymentGateway) -> Unit,
    ) = testApplication {
        val persistence = PaymentPersistence.inMemory("route_${Base58.randomString(8)}")
        val gateway = ScenarioPaymentGateway()

        application {
            ktorHttpOutboxModule(persistence, gateway)
        }

        test(gateway)
    }
}
