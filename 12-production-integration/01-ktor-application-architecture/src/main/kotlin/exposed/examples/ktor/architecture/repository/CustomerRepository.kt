package exposed.examples.ktor.architecture.repository

import exposed.examples.ktor.architecture.model.CreateCustomerCommand
import exposed.examples.ktor.architecture.model.CustomerRecord

/**
 * 서비스 계층이 사용하는 고객 영속성 계약이다.
 */
internal interface CustomerRepository {
    suspend fun create(command: CreateCustomerCommand): CustomerRecord
    suspend fun findById(id: Long): CustomerRecord?
    suspend fun findAll(): List<CustomerRecord>
    suspend fun count(): Long
}
