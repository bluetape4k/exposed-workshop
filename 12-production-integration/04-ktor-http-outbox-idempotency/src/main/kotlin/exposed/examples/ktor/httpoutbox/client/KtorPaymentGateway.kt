package exposed.examples.ktor.httpoutbox.client

import exposed.examples.ktor.httpoutbox.model.CreatePaymentCommand
import exposed.examples.ktor.httpoutbox.model.ExternalPaymentRequest
import exposed.examples.ktor.httpoutbox.model.ExternalPaymentResponse
import exposed.examples.ktor.httpoutbox.model.ExternalPaymentResult
import exposed.examples.ktor.httpoutbox.model.PermanentExternalPaymentException
import exposed.examples.ktor.httpoutbox.model.RetryableExternalPaymentException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.Closeable

internal class KtorPaymentGateway(
    private val baseUrl: String,
    private val client: HttpClient = defaultClient(),
) : PaymentGateway,
    Closeable {

    override suspend fun charge(command: CreatePaymentCommand): ExternalPaymentResult {
        try {
            val response = client.post("$baseUrl/payments") {
                header(IDEMPOTENCY_KEY_HEADER, command.idempotencyKey)
                setBody(ExternalPaymentRequest(command.orderId, command.amountCents))
            }.body<ExternalPaymentResponse>()

            return ExternalPaymentResult(
                externalId = response.externalId.takeIf { it.isNotBlank() }
                    ?: "ktor-${command.idempotencyKey}"
            )
        } catch (e: ClientRequestException) {
            throw PermanentExternalPaymentException("External payment request was rejected", e)
        } catch (e: ServerResponseException) {
            throw RetryableExternalPaymentException("External payment service is temporarily unavailable", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw RetryableExternalPaymentException("External payment service could not be reached", e)
        }
    }

    private companion object {
        private const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
    }

    override fun close() {
        client.close()
    }
}

private fun defaultClient(): HttpClient =
    HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }
    }
