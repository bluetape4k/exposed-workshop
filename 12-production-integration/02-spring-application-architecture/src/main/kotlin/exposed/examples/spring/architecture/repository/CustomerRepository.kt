package exposed.examples.spring.architecture.repository

import exposed.examples.spring.architecture.model.CreateCustomerCommand
import exposed.examples.spring.architecture.model.CustomerRecord

/**
 * Customer persistence contract used by the Spring service layer.
 */
internal interface CustomerRepository {
    fun create(command: CreateCustomerCommand): CustomerRecord
    fun findById(id: Long): CustomerRecord?
    fun findAll(): List<CustomerRecord>
    fun count(): Long
    fun deleteAll()
}

