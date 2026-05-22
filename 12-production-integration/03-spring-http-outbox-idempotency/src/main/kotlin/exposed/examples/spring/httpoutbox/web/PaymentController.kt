package exposed.examples.spring.httpoutbox.web

import exposed.examples.spring.httpoutbox.model.CreatePaymentRequest
import exposed.examples.spring.httpoutbox.model.PaymentResponse
import exposed.examples.spring.httpoutbox.model.PaymentsResponse
import exposed.examples.spring.httpoutbox.service.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/payments")
internal class PaymentController(
    private val service: PaymentService,
) {

    @PostMapping
    fun submit(@RequestBody request: CreatePaymentRequest): ResponseEntity<PaymentResponse> {
        val response = service.submit(request)
        val status = if (response.duplicate) HttpStatus.OK else HttpStatus.CREATED
        return ResponseEntity.status(status).body(response)
    }

    @PostMapping("/{id}/retry")
    fun retry(@PathVariable id: Long): PaymentResponse =
        service.retry(id)

    @GetMapping("/{id}")
    fun find(@PathVariable id: Long): PaymentResponse =
        service.find(id)

    @GetMapping
    fun findAll(): PaymentsResponse =
        service.findAll()
}
