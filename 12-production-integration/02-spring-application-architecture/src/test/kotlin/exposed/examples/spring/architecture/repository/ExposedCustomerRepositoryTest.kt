package exposed.examples.spring.architecture.repository

import exposed.examples.spring.architecture.model.CreateCustomerCommand
import exposed.examples.spring.architecture.persistence.CustomerPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedCustomerRepositoryTest {

    @Test
    fun `repository creates and finds customers through Exposed transactions`() {
        withRepository { repository ->
            repository.count() shouldBeEqualTo 0L

            val created = repository.create(CreateCustomerCommand("Alice", "alice@example.com"))

            repository.findById(created.id) shouldBeEqualTo created
            repository.findAll() shouldHaveSize 1
            repository.count() shouldBeEqualTo 1L
        }
    }

    @Test
    fun `deleteAll resets table state for application tests`() {
        withRepository { repository ->
            repository.create(CreateCustomerCommand("Alice", "alice@example.com"))
            repository.deleteAll()

            repository.findAll() shouldHaveSize 0
        }
    }

    private fun withRepository(test: (ExposedCustomerRepository) -> Unit) {
        CustomerPersistence.inMemory("repo_${Base58.randomString(8)}").use { persistence ->
            test(ExposedCustomerRepository(persistence.database))
        }
    }
}

