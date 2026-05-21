package exposed.examples.spring.architecture.service

import exposed.examples.spring.architecture.model.CreateCustomerCommand
import exposed.examples.spring.architecture.model.CreateCustomerRequest
import exposed.examples.spring.architecture.model.CustomerRecord
import exposed.examples.spring.architecture.model.CustomerValidationException
import exposed.examples.spring.architecture.repository.CustomerRepository
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerServiceTest {

    @Test
    fun `create normalizes customer input before persistence`() {
        val repository = FakeCustomerRepository()
        val service = CustomerService(repository)

        val created = service.create(CreateCustomerRequest(" Alice ", "ALICE@EXAMPLE.COM "))

        created.name shouldBeEqualTo "Alice"
        created.email shouldBeEqualTo "alice@example.com"
        repository.lastCommand shouldBeEqualTo CreateCustomerCommand("Alice", "alice@example.com")
    }

    @Test
    fun `blank name is rejected before persistence`() {
        val service = CustomerService(FakeCustomerRepository())

        val error = try {
            service.create(CreateCustomerRequest(" ", "alice@example.com"))
            throw AssertionError("CustomerValidationException was expected")
        } catch (e: CustomerValidationException) {
            e
        }

        error.message shouldBeEqualTo "name must not be blank"
    }

    private class FakeCustomerRepository : CustomerRepository {
        var lastCommand: CreateCustomerCommand? = null

        override fun create(command: CreateCustomerCommand): CustomerRecord {
            lastCommand = command
            return CustomerRecord(id = 1L, name = command.name, email = command.email)
        }

        override fun findById(id: Long): CustomerRecord? =
            null

        override fun findAll(): List<CustomerRecord> =
            emptyList()

        override fun count(): Long =
            0L

        override fun deleteAll() = Unit
    }
}
