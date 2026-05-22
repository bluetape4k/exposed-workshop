package exposed.examples.spring.httpoutbox.repository

import exposed.examples.spring.httpoutbox.model.CreatePaymentCommand
import exposed.examples.spring.httpoutbox.model.CreatePaymentResult
import exposed.examples.spring.httpoutbox.model.PaymentRecord

internal interface PaymentOutboxRepository {
    fun createPending(command: CreatePaymentCommand): CreatePaymentResult
    fun findById(id: Long): PaymentRecord?
    fun findAll(): List<PaymentRecord>
    fun markSucceeded(id: Long, externalId: String): PaymentRecord
    fun markRetryableFailure(id: Long, message: String): PaymentRecord
    fun markPermanentFailure(id: Long, message: String): PaymentRecord
    fun deleteAll()
}
