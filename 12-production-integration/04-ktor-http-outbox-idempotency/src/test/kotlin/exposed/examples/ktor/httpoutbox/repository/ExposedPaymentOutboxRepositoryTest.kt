package exposed.examples.ktor.httpoutbox.repository

import exposed.examples.ktor.httpoutbox.model.CreatePaymentCommand
import exposed.examples.ktor.httpoutbox.model.PaymentStatus
import exposed.examples.ktor.httpoutbox.persistence.PaymentPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedPaymentOutboxRepositoryTest {

    @Test
    fun `create pending request then mark success`() = runTest {
        PaymentPersistence.inMemory("ktor_repo_${Base58.randomString(8)}").use { persistence ->
            val repository = ExposedPaymentOutboxRepository(persistence.database)

            val created = repository.createPending(command("pay-1"))
            created.inserted shouldBeEqualTo true
            created.record.status shouldBeEqualTo PaymentStatus.PENDING

            val succeeded = repository.markSucceeded(created.record.id, "ext-1")

            succeeded.status shouldBeEqualTo PaymentStatus.SUCCEEDED
            succeeded.attempts shouldBeEqualTo 1
            succeeded.externalId shouldBeEqualTo "ext-1"
        }
    }

    @Test
    fun `duplicate idempotency key returns stored record`() = runTest {
        PaymentPersistence.inMemory("ktor_repo_${Base58.randomString(8)}").use { persistence ->
            val repository = ExposedPaymentOutboxRepository(persistence.database)

            val first = repository.createPending(command("same-key"))
            val duplicate = repository.createPending(command("same-key"))

            duplicate.inserted shouldBeEqualTo false
            duplicate.record.id shouldBeEqualTo first.record.id
        }
    }

    @Test
    fun `concurrent duplicate idempotency key returns one stored record`() = runTest {
        PaymentPersistence.inMemory("ktor_repo_${Base58.randomString(8)}").use { persistence ->
            val repository = ExposedPaymentOutboxRepository(persistence.database)
            val start = CompletableDeferred<Unit>()
            val results = coroutineScope {
                val jobs = (1..8).map {
                    async(Dispatchers.Default) {
                        start.await()
                        repository.createPending(command("concurrent-key"))
                    }
                }
                start.complete(Unit)
                jobs.awaitAll()
            }

            results.count { it.inserted } shouldBeEqualTo 1
            results.map { it.record.id }.toSet() shouldHaveSize 1
            repository.findAll() shouldHaveSize 1
        }
    }

    private fun command(idempotencyKey: String): CreatePaymentCommand =
        CreatePaymentCommand(
            orderId = "order-$idempotencyKey",
            amountCents = 1_500,
            idempotencyKey = idempotencyKey
        )
}
