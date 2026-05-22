package exposed.examples.ktor.httpoutbox.client

import exposed.examples.ktor.httpoutbox.model.CreatePaymentCommand
import exposed.examples.ktor.httpoutbox.model.ExternalPaymentResult

internal interface PaymentGateway {
    suspend fun charge(command: CreatePaymentCommand): ExternalPaymentResult
}
