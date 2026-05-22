package exposed.examples.spring.httpoutbox.repository

import exposed.examples.spring.httpoutbox.model.CreatePaymentCommand
import exposed.examples.spring.httpoutbox.model.CreatePaymentResult
import exposed.examples.spring.httpoutbox.model.PaymentStatus
import exposed.examples.spring.httpoutbox.persistence.PaymentPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedPaymentOutboxRepositoryTest {

    @Test
    fun `create pending request then mark success`() {
        PaymentPersistence.inMemory("spring_repo_${Base58.randomString(8)}").use { persistence ->
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
    fun `duplicate idempotency key returns stored record`() {
        PaymentPersistence.inMemory("spring_repo_${Base58.randomString(8)}").use { persistence ->
            val repository = ExposedPaymentOutboxRepository(persistence.database)

            val first = repository.createPending(command("same-key"))
            val duplicate = repository.createPending(command("same-key"))

            duplicate.inserted shouldBeEqualTo false
            duplicate.record.id shouldBeEqualTo first.record.id
        }
    }

    @Test
    fun `concurrent duplicate idempotency key returns one stored record`() {
        PaymentPersistence.inMemory("spring_repo_${Base58.randomString(8)}").use { persistence ->
            val repository = ExposedPaymentOutboxRepository(persistence.database)
            val pool = Executors.newFixedThreadPool(8)
            val start = CountDownLatch(1)

            try {
                val futures = (1..8).map {
                    pool.submit<CreatePaymentResult> {
                        start.await()
                        repository.createPending(command("concurrent-key"))
                    }
                }

                start.countDown()
                val results = futures.map { it.get(5, TimeUnit.SECONDS) }

                results.count { it.inserted } shouldBeEqualTo 1
                results.map { it.record.id }.toSet() shouldHaveSize 1
                repository.findAll() shouldHaveSize 1
            } finally {
                pool.shutdownNow()
            }
        }
    }

    private fun command(idempotencyKey: String): CreatePaymentCommand =
        CreatePaymentCommand(
            orderId = "order-$idempotencyKey",
            amountCents = 1_500,
            idempotencyKey = idempotencyKey
        )
}
