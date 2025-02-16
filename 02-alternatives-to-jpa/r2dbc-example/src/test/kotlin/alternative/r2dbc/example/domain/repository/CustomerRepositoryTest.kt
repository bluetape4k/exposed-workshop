package alternative.r2dbc.example.domain.repository

import alternative.r2dbc.example.AbstractR2dbcTest
import alternative.r2dbc.example.domain.model.Customer
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class CustomerRepositoryTest(
    @Autowired private val customerRepository: CustomerRepository,
    @Autowired private val database: DatabaseClient,
): AbstractR2dbcTest() {

    companion object: KLogging()

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            val statements = listOf(
                "DROP TABLE IF EXISTS customer;",
                """
                CREATE TABLE customer (
                    id SERIAL PRIMARY KEY, 
                    firstname VARCHAR(100) NOT NULL, 
                    lastname VARCHAR(100) NOT NULL
                );
                """.trimIndent(),
            )

            statements.forEach {
                database.sql(it)
                    .fetch()
                    .rowsUpdated()
                    .awaitSingle()
            }
        }
    }

    @Test
    fun `context loading`() {
        database.shouldNotBeNull()
    }

    @Test
    fun `execute find all`() = runSuspendIO {
        val dave = Customer("Dave", "Matthews")
        val carter = Customer("Carter", "Beauford")

        insertCustomers(dave, carter)

        val customers = customerRepository.findAll().toList()

        customers shouldBeEqualTo listOf(dave, carter)
    }

    @Test
    fun `execute custom function`() = runTest {
        val dave = Customer("Dave", "Matthews")
        val carter = Customer("Carter", "Beauford")

        insertCustomers(dave, carter)

        val customer = customerRepository.findByFirstname("Carter").first()
        customer.shouldNotBeNull() shouldBeEqualTo carter
    }

    @Test
    fun `execute annotated query`() = runTest {
        val dave = Customer("Dave", "Matthews")
        val carter = Customer("Carter", "Beauford")

        insertCustomers(dave, carter)

        val customer = customerRepository.findByLastname("Matthews").first()
        customer.shouldNotBeNull() shouldBeEqualTo dave
    }

    private suspend fun insertCustomers(vararg customers: Customer) {
        customerRepository.saveAll(customers.toList()).collect()
    }
}
