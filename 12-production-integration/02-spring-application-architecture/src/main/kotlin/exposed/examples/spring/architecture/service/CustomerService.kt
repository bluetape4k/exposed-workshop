package exposed.examples.spring.architecture.service

import exposed.examples.spring.architecture.model.CreateCustomerCommand
import exposed.examples.spring.architecture.model.CreateCustomerRequest
import exposed.examples.spring.architecture.model.CustomerResponse
import exposed.examples.spring.architecture.model.CustomerValidationException
import exposed.examples.spring.architecture.model.CustomersResponse
import exposed.examples.spring.architecture.model.toResponse
import exposed.examples.spring.architecture.repository.CustomerRepository

/**
 * 고객 유스케이스와 호출자 입력 검증을 담당한다.
 */
internal class CustomerService(
    private val repository: CustomerRepository,
) {
    fun create(request: CreateCustomerRequest): CustomerResponse {
        val command = CreateCustomerCommand(
            name = request.name.normalizeName(),
            email = request.email.normalizeEmail()
        )
        return repository.create(command).toResponse()
    }

    fun find(id: Long): CustomerResponse =
        repository.findById(id)?.toResponse()
            ?: throw NoSuchElementException("Customer $id was not found")

    fun findAll(): CustomersResponse =
        CustomersResponse(repository.findAll().map { it.toResponse() })

    fun count(): Long =
        repository.count()

    fun deleteAll() {
        repository.deleteAll()
    }

    private fun String.normalizeName(): String {
        val normalized = trim()
        if (normalized.isBlank()) {
            throw CustomerValidationException("name must not be blank")
        }
        if (normalized.length > 80) {
            throw CustomerValidationException("name must be 80 characters or less")
        }
        return normalized
    }

    private fun String.normalizeEmail(): String {
        val normalized = trim().lowercase()
        if (normalized.isBlank()) {
            throw CustomerValidationException("email must not be blank")
        }
        if (normalized.length > 120) {
            throw CustomerValidationException("email must be 120 characters or less")
        }
        if ("@" !in normalized) {
            throw CustomerValidationException("email must contain @")
        }
        return normalized
    }
}

