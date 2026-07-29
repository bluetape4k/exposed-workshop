package exposed.examples.ktor.architecture.service

import exposed.examples.ktor.architecture.model.CreateCustomerCommand
import exposed.examples.ktor.architecture.model.CreateCustomerRequest
import exposed.examples.ktor.architecture.model.CustomerResponse
import exposed.examples.ktor.architecture.model.CustomerValidationException
import exposed.examples.ktor.architecture.model.CustomersResponse
import exposed.examples.ktor.architecture.model.toResponse
import exposed.examples.ktor.architecture.repository.CustomerRepository

/**
 * 고객 유스케이스와 호출자 입력 검증을 담당한다.
 */
internal class CustomerService(
    private val repository: CustomerRepository,
) {
    suspend fun create(request: CreateCustomerRequest): CustomerResponse {
        val command = CreateCustomerCommand(
            name = request.name.normalizeName(),
            email = request.email.normalizeEmail()
        )
        return repository.create(command).toResponse()
    }

    suspend fun find(id: Long): CustomerResponse =
        repository.findById(id)?.toResponse()
            ?: throw NoSuchElementException("Customer $id was not found")

    suspend fun findAll(): CustomersResponse =
        CustomersResponse(repository.findAll().map { it.toResponse() })

    suspend fun count(): Long =
        repository.count()

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
