package exposed.examples.spring.architecture.repository

import exposed.examples.spring.architecture.model.CreateCustomerCommand
import exposed.examples.spring.architecture.model.CustomerRecord

/**
 * Spring 서비스 계층이 사용하는 고객 영속성 계약이다.
 */
internal interface CustomerRepository {
    fun create(command: CreateCustomerCommand): CustomerRecord
    fun findById(id: Long): CustomerRecord?
    fun findAll(): List<CustomerRecord>
    fun count(): Long
    fun deleteAll()
}

