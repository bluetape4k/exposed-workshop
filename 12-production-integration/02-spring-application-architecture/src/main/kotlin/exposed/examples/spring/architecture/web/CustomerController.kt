package exposed.examples.spring.architecture.web

import exposed.examples.spring.architecture.model.CreateCustomerRequest
import exposed.examples.spring.architecture.model.CustomerResponse
import exposed.examples.spring.architecture.model.CustomersResponse
import exposed.examples.spring.architecture.service.CustomerService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/customers", produces = [MediaType.APPLICATION_JSON_VALUE])
internal class CustomerController(
    private val service: CustomerService,
) {

    @GetMapping
    fun findAll(): CustomersResponse =
        service.findAll()

    @GetMapping("/{id}")
    fun find(@PathVariable id: Long): CustomerResponse =
        service.find(id)

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun create(@RequestBody request: CreateCustomerRequest): ResponseEntity<CustomerResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))
}

