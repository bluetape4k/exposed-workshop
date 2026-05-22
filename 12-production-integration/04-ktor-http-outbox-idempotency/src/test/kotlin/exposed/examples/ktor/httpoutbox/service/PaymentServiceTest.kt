package exposed.examples.ktor.httpoutbox.service

import exposed.examples.ktor.httpoutbox.client.PaymentGateway
import exposed.examples.ktor.httpoutbox.model.CreatePaymentCommand
import exposed.examples.ktor.httpoutbox.model.CreatePaymentRequest
import exposed.examples.ktor.httpoutbox.model.ExternalPaymentResult
import exposed.examples.ktor.httpoutbox.model.PaymentStatus
import exposed.examples.ktor.httpoutbox.model.PaymentValidationException
import exposed.examples.ktor.httpoutbox.model.PermanentExternalPaymentException
import exposed.examples.ktor.httpoutbox.model.RetryableExternalPaymentException
import exposed.examples.ktor.httpoutbox.persistence.PaymentPersistence
import exposed.examples.ktor.httpoutbox.repository.ExposedPaymentOutboxRepository
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.codec.Base58
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.ArrayDeque

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentServiceTest {

    @Test
    fun `successful payment is persisted before external success is recorded`() = runTest {
        withService { service, gateway ->
            gateway.succeed("key-1", "ext-1")

            val response = service.submit(CreatePaymentRequest(" order-1 ", 2_500, " key-1 "))

            response.status shouldBeEqualTo PaymentStatus.SUCCEEDED
            response.attempts shouldBeEqualTo 1
            response.externalId shouldBeEqualTo "ext-1"
            gateway.commands.single() shouldBeEqualTo CreatePaymentCommand("order-1", 2_500, "key-1")
        }
    }

    @Test
    fun `duplicate idempotency key returns existing payment without second external call`() = runTest {
        withService { service, gateway ->
            gateway.succeed("key-2", "ext-2")

            val first = service.submit(CreatePaymentRequest("order-2", 2_500, "key-2"))
            val duplicate = service.submit(CreatePaymentRequest("order-2", 2_500, "key-2"))

            duplicate.id shouldBeEqualTo first.id
            duplicate.duplicate shouldBeEqualTo true
            gateway.commands.size shouldBeEqualTo 1
        }
    }

    @Test
    fun `retryable failure can be retried to success`() = runTest {
        withService { service, gateway ->
            gateway.retryableThenSuccess("retry-key", "ext-retry")

            val failed = service.submit(CreatePaymentRequest("order-retry", 1_000, "retry-key"))
            failed.status shouldBeEqualTo PaymentStatus.RETRYABLE_FAILED

            val retried = service.retry(failed.id)

            retried.status shouldBeEqualTo PaymentStatus.SUCCEEDED
            retried.attempts shouldBeEqualTo 2
            retried.externalId shouldBeEqualTo "ext-retry"
        }
    }

    @Test
    fun `permanent failure is not retryable`() = runTest {
        withService { service, gateway ->
            gateway.permanent("bad-key")
            val failed = service.submit(CreatePaymentRequest("order-bad", 1_000, "bad-key"))

            val error = try {
                service.retry(failed.id)
                throw AssertionError("PaymentValidationException was expected")
            } catch (e: PaymentValidationException) {
                e
            }

            failed.status shouldBeEqualTo PaymentStatus.PERMANENT_FAILED
            error.message shouldBeEqualTo "only retryable failed payments can be retried"
        }
    }

    private suspend fun withService(test: suspend (PaymentService, ScenarioPaymentGateway) -> Unit) {
        PaymentPersistence.inMemory("ktor_service_${Base58.randomString(8)}").use { persistence ->
            val gateway = ScenarioPaymentGateway()
            val repository = ExposedPaymentOutboxRepository(persistence.database)
            test(PaymentService(repository, gateway), gateway)
        }
    }
}

internal class ScenarioPaymentGateway : PaymentGateway {
    val commands = mutableListOf<CreatePaymentCommand>()
    private val outcomes = mutableMapOf<String, ArrayDeque<() -> ExternalPaymentResult>>()

    override suspend fun charge(command: CreatePaymentCommand): ExternalPaymentResult {
        commands += command
        val next = outcomes[command.idempotencyKey]?.poll()
        return next?.invoke() ?: ExternalPaymentResult("ext-${command.idempotencyKey}")
    }

    fun succeed(
        idempotencyKey: String,
        externalId: String,
    ) {
        outcomes[idempotencyKey] = ArrayDeque(listOf({ ExternalPaymentResult(externalId) }))
    }

    fun retryableThenSuccess(
        idempotencyKey: String,
        externalId: String,
    ) {
        outcomes[idempotencyKey] = ArrayDeque(
            listOf(
                { throw RetryableExternalPaymentException("temporary failure") },
                { ExternalPaymentResult(externalId) }
            )
        )
    }

    fun permanent(idempotencyKey: String) {
        outcomes[idempotencyKey] = ArrayDeque(listOf({ throw PermanentExternalPaymentException("permanent failure") }))
    }
}
