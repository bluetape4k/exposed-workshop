package exposed.examples.ktor.architecture.service

import exposed.examples.ktor.architecture.model.CreateCustomerCommand
import exposed.examples.ktor.architecture.model.CreateCustomerRequest
import exposed.examples.ktor.architecture.model.CustomerRecord
import exposed.examples.ktor.architecture.model.CustomerValidationException
import exposed.examples.ktor.architecture.repository.CustomerRepository
import io.bluetape4k.assertions.shouldBeEqualTo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerServiceTest {

    @Test
    fun `create normalizes customer input before persistence`() = runTest {
        val repository = FakeCustomerRepository()
        val service = CustomerService(repository)

        val created = service.create(CreateCustomerRequest(" Alice ", "ALICE@EXAMPLE.COM "))

        created.name shouldBeEqualTo "Alice"
        created.email shouldBeEqualTo "alice@example.com"
        repository.createdCommands.single() shouldBeEqualTo CreateCustomerCommand("Alice", "alice@example.com")
    }

    @Test
    fun `create rejects invalid caller input`() = runTest {
        val service = CustomerService(FakeCustomerRepository())

        val blankName = captureFailure<CustomerValidationException> {
            service.create(CreateCustomerRequest(" ", "alice@example.com"))
        }
        blankName.message shouldBeEqualTo "name must not be blank"

        val invalidEmail = captureFailure<CustomerValidationException> {
            service.create(CreateCustomerRequest("Alice", "alice.example.com"))
        }
        invalidEmail.message shouldBeEqualTo "email must contain @"
    }

    @Test
    fun `find maps missing repository record to not found`() = runTest {
        val service = CustomerService(FakeCustomerRepository())

        val failure = captureFailure<NoSuchElementException> {
            service.find(404)
        }

        failure.message shouldBeEqualTo "Customer 404 was not found"
    }

    @Test
    fun `findAll returns repository records as responses`() = runTest {
        val repository = FakeCustomerRepository(
            records = mutableMapOf(
                1L to CustomerRecord(1L, "Alice", "alice@example.com"),
                2L to CustomerRecord(2L, "Bob", "bob@example.com")
            )
        )
        val service = CustomerService(repository)

        val response = service.findAll()

        response.customers.size shouldBeEqualTo 2
        response.customers.first().name shouldBeEqualTo "Alice"
    }

    private suspend inline fun <reified T : Throwable> captureFailure(
        noinline block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) {
                return e
            }
            throw e
        }
        throw AssertionError("Expected ${T::class.simpleName}")
    }

    private class FakeCustomerRepository(
        private val records: MutableMap<Long, CustomerRecord> = mutableMapOf(),
    ) : CustomerRepository {

        val createdCommands = mutableListOf<CreateCustomerCommand>()

        override suspend fun create(command: CreateCustomerCommand): CustomerRecord {
            createdCommands += command
            val id = (records.keys.maxOrNull() ?: 0L) + 1
            val record = CustomerRecord(id = id, name = command.name, email = command.email)
            records[id] = record
            return record
        }

        override suspend fun findById(id: Long): CustomerRecord? =
            records[id]

        override suspend fun findAll(): List<CustomerRecord> =
            records.values.sortedBy { it.id }

        override suspend fun count(): Long =
            records.size.toLong()
    }
}
