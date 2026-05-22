package exposed.examples.spring.httpoutbox.client

import exposed.examples.spring.httpoutbox.model.CreatePaymentCommand
import exposed.examples.spring.httpoutbox.model.ExternalPaymentRequest
import exposed.examples.spring.httpoutbox.model.ExternalPaymentResponse
import exposed.examples.spring.httpoutbox.model.ExternalPaymentResult
import exposed.examples.spring.httpoutbox.model.PermanentExternalPaymentException
import exposed.examples.spring.httpoutbox.model.RetryableExternalPaymentException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

internal class RestClientPaymentGateway(
    private val restClient: RestClient,
) : PaymentGateway {

    override fun charge(command: CreatePaymentCommand): ExternalPaymentResult {
        try {
            val response = restClient.post()
                .uri("/payments")
                .header(IDEMPOTENCY_KEY_HEADER, command.idempotencyKey)
                .body(ExternalPaymentRequest(command.orderId, command.amountCents))
                .retrieve()
                .body(ExternalPaymentResponse::class.java)

            return ExternalPaymentResult(
                externalId = response?.externalId?.takeIf { it.isNotBlank() }
                    ?: "spring-${command.idempotencyKey}"
            )
        } catch (e: RestClientResponseException) {
            if (e.statusCode.is4xxClientError) {
                throw PermanentExternalPaymentException("External payment request was rejected", e)
            }
            throw RetryableExternalPaymentException("External payment service is temporarily unavailable", e)
        } catch (e: ResourceAccessException) {
            throw RetryableExternalPaymentException("External payment service could not be reached", e)
        }
    }

    private companion object {
        private const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
    }
}
