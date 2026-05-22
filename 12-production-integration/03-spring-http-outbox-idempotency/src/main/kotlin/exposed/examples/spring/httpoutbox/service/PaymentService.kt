package exposed.examples.spring.httpoutbox.service

import exposed.examples.spring.httpoutbox.client.PaymentGateway
import exposed.examples.spring.httpoutbox.model.CreatePaymentCommand
import exposed.examples.spring.httpoutbox.model.CreatePaymentRequest
import exposed.examples.spring.httpoutbox.model.PaymentResponse
import exposed.examples.spring.httpoutbox.model.PaymentStatus
import exposed.examples.spring.httpoutbox.model.PaymentValidationException
import exposed.examples.spring.httpoutbox.model.PaymentsResponse
import exposed.examples.spring.httpoutbox.model.PermanentExternalPaymentException
import exposed.examples.spring.httpoutbox.model.RetryableExternalPaymentException
import exposed.examples.spring.httpoutbox.model.toResponse
import exposed.examples.spring.httpoutbox.repository.PaymentOutboxRepository

internal class PaymentService(
    private val repository: PaymentOutboxRepository,
    private val paymentGateway: PaymentGateway,
) {

    fun submit(request: CreatePaymentRequest): PaymentResponse {
        val command = request.toCommand()
        val created = repository.createPending(command)
        if (!created.inserted) {
            return created.record.toResponse(duplicate = true)
        }
        return dispatch(created.record.id, command)
    }

    fun retry(id: Long): PaymentResponse {
        val record = repository.findById(id)
            ?: throw NoSuchElementException("Payment request $id was not found")
        if (record.status != PaymentStatus.RETRYABLE_FAILED) {
            throw PaymentValidationException("only retryable failed payments can be retried")
        }
        return dispatch(
            id = record.id,
            command = CreatePaymentCommand(
                orderId = record.orderId,
                amountCents = record.amountCents,
                idempotencyKey = record.idempotencyKey
            )
        )
    }

    fun find(id: Long): PaymentResponse =
        repository.findById(id)?.toResponse()
            ?: throw NoSuchElementException("Payment request $id was not found")

    fun findAll(): PaymentsResponse =
        PaymentsResponse(repository.findAll().map { it.toResponse() })

    fun deleteAll() {
        repository.deleteAll()
    }

    private fun dispatch(
        id: Long,
        command: CreatePaymentCommand,
    ): PaymentResponse =
        try {
            val result = paymentGateway.charge(command)
            repository.markSucceeded(id, result.externalId).toResponse()
        } catch (e: PermanentExternalPaymentException) {
            repository.markPermanentFailure(id, e.message ?: "permanent external payment failure").toResponse()
        } catch (e: RetryableExternalPaymentException) {
            repository.markRetryableFailure(id, e.message ?: "retryable external payment failure").toResponse()
        }

    private fun CreatePaymentRequest.toCommand(): CreatePaymentCommand =
        CreatePaymentCommand(
            orderId = orderId.normalize("orderId", 80),
            amountCents = amountCents.requirePositiveAmount(),
            idempotencyKey = idempotencyKey.normalize("idempotencyKey", 120)
        )

    private fun String.normalize(
        fieldName: String,
        maxLength: Int,
    ): String {
        val normalized = trim()
        if (normalized.isBlank()) {
            throw PaymentValidationException("$fieldName must not be blank")
        }
        if (normalized.length > maxLength) {
            throw PaymentValidationException("$fieldName must be $maxLength characters or less")
        }
        return normalized
    }

    private fun Long.requirePositiveAmount(): Long {
        if (this <= 0) {
            throw PaymentValidationException("amountCents must be positive")
        }
        return this
    }
}
