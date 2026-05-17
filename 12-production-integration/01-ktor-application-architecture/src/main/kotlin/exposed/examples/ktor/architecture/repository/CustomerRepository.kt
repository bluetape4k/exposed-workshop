package exposed.examples.ktor.architecture.repository

import exposed.examples.ktor.architecture.model.CreateCustomerCommand
import exposed.examples.ktor.architecture.model.CustomerRecord

/**
 * Customer persistence contract used by the service layer.
 */
internal interface CustomerRepository {
    suspend fun create(command: CreateCustomerCommand): CustomerRecord
    suspend fun findById(id: Long): CustomerRecord?
    suspend fun findAll(): List<CustomerRecord>
    suspend fun count(): Long
}
