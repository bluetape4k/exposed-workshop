package exposed.examples.cockroachdb.retry

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.exposed.cockroachdb.CockroachDatabase
import io.bluetape4k.exposed.cockroachdb.isCockroachRetryableTransactionError
import io.bluetape4k.testcontainers.database.CockroachServer
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.sql.SQLException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class CockroachRetryWorkshopTest {

    private val cockroach: CockroachServer by lazy { CockroachServer.Launcher.cockroach }

    private val db: Database by lazy {
        CockroachDatabase.connect(
            jdbcUrl = cockroach.url,
            user = cockroach.username ?: CockroachServer.USERNAME,
            password = cockroach.password ?: CockroachServer.PASSWORD,
        )
    }

    private val service: CockroachInventoryService by lazy {
        CockroachInventoryService(db = db)
    }

    @BeforeAll
    fun waitForCockroachReady() {
        service.bootstrapSchema()
    }

    @Test
    fun `schema bootstrap creates the initial inventory row`() {
        val snapshot = service.bootstrapSchema(initialSku = "schema-check", initialQuantity = 7)

        snapshot shouldBeEqualTo InventorySnapshot(
            sku = "schema-check",
            quantityOnHand = 7,
            version = 0L,
        )
        service.ledgerCount("schema-check") shouldBeEqualTo 0L
    }

    @Test
    fun `successful reservation commits inventory update and ledger entry`() {
        service.bootstrapSchema(initialSku = "success", initialQuantity = 10)

        val snapshot = service.reserve(sku = "success", quantity = 3, reason = "checkout")

        snapshot shouldBeEqualTo InventorySnapshot(
            sku = "success",
            quantityOnHand = 7,
            version = 1L,
        )
        service.ledgerCount("success") shouldBeEqualTo 1L
    }

    @Test
    fun `retryable serialization conflict reruns the whole reservation transaction`() {
        service.bootstrapSchema(initialSku = "retry", initialQuantity = 10)
        var attempts = 0

        val snapshot = service.reserve(
            sku = "retry",
            quantity = 4,
            reason = "checkout",
        ) { attempt ->
            attempts = attempt
            if (attempt == 1) {
                throw cockroachRetryableSerializationFailure()
            }
        }

        attempts shouldBeEqualTo 2
        snapshot shouldBeEqualTo InventorySnapshot(
            sku = "retry",
            quantityOnHand = 6,
            version = 1L,
        )
        service.ledgerCount("retry") shouldBeEqualTo 1L
    }

    @Test
    fun `non retryable sql failure is not retried and leaves data unchanged`() {
        service.bootstrapSchema(initialSku = "non-retry", initialQuantity = 10)
        var attempts = 0
        val duplicateKey = SQLException("duplicate key value violates unique constraint", "23505")

        val failure = assertFailsWith<SQLException> {
            service.reserve(sku = "non-retry", quantity = 2) { attempt ->
                attempts = attempt
                throw duplicateKey
            }
        }

        failure shouldBeEqualTo duplicateKey
        attempts shouldBeEqualTo 1
        service.inventory("non-retry") shouldBeEqualTo InventorySnapshot(
            sku = "non-retry",
            quantityOnHand = 10,
            version = 0L,
        )
        service.ledgerCount("non-retry") shouldBeEqualTo 0L
    }

    @Test
    fun `retry predicate documents the CockroachDB retry signature`() {
        cockroachRetryableSerializationFailure()
            .isCockroachRetryableTransactionError()
            .shouldBeTrue()
    }
}
