package exposed.examples.ktor.httpoutbox.repository

import exposed.examples.ktor.httpoutbox.model.CreatePaymentCommand
import exposed.examples.ktor.httpoutbox.model.CreatePaymentResult
import exposed.examples.ktor.httpoutbox.model.PaymentRecord

internal interface PaymentOutboxRepository {
    suspend fun createPending(command: CreatePaymentCommand): CreatePaymentResult
    suspend fun findById(id: Long): PaymentRecord?
    suspend fun findAll(): List<PaymentRecord>
    suspend fun markSucceeded(id: Long, externalId: String): PaymentRecord
    suspend fun markRetryableFailure(id: Long, message: String): PaymentRecord
    suspend fun markPermanentFailure(id: Long, message: String): PaymentRecord
    suspend fun deleteAll()
}
