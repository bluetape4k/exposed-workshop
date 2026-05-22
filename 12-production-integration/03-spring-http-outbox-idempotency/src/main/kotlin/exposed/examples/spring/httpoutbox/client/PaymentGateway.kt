package exposed.examples.spring.httpoutbox.client

import exposed.examples.spring.httpoutbox.model.CreatePaymentCommand
import exposed.examples.spring.httpoutbox.model.ExternalPaymentResult

internal interface PaymentGateway {
    fun charge(command: CreatePaymentCommand): ExternalPaymentResult
}
