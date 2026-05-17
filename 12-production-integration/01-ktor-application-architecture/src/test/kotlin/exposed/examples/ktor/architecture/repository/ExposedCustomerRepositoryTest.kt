package exposed.examples.ktor.architecture.repository

import exposed.examples.ktor.architecture.model.CreateCustomerCommand
import exposed.examples.ktor.architecture.persistence.CustomerPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedCustomerRepositoryTest {

    @Test
    fun `repository creates and reads customers through Exposed JDBC`(): Unit = runBlocking {
        withRepository { repository ->
            repository.count() shouldBeEqualTo 0L

            val created = repository.create(CreateCustomerCommand("Alice", "alice@example.com"))

            repository.count() shouldBeEqualTo 1L
            repository.findById(created.id) shouldBeEqualTo created
            repository.findAll() shouldBeEqualTo listOf(created)
        }
    }

    @Test
    fun `parallel creates are isolated behind repository transaction boundary`(): Unit = runBlocking {
        withRepository { repository ->
            val created = coroutineScope {
                (1..16).map { index ->
                    async {
                        repository.create(CreateCustomerCommand("Customer $index", "customer$index@example.com"))
                    }
                }.awaitAll()
            }

            created.map { it.id }.toSet() shouldHaveSize 16
            repository.count() shouldBeEqualTo 16L
        }
    }

    private suspend fun withRepository(
        block: suspend (ExposedCustomerRepository) -> Unit,
    ) {
        CustomerPersistence.inMemory("repository_${Base58.randomString(8)}").use { persistence ->
            block(ExposedCustomerRepository(persistence.database))
        }
    }
}
