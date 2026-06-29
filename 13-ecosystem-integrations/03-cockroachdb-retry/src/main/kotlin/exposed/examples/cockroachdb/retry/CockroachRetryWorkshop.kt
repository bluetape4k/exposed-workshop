package exposed.examples.cockroachdb.retry

import io.bluetape4k.exposed.cockroachdb.CockroachTransactionRetryOptions
import io.bluetape4k.exposed.cockroachdb.withCockroachTransaction
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.io.Serializable
import java.sql.Connection
import java.sql.SQLException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Inventory row returned by the CockroachDB retry workshop.
 */
data class InventorySnapshot(
    val sku: String,
    val quantityOnHand: Int,
    val version: Long,
): Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Testable hook for showing how CockroachDB asks a client to retry a whole
 * serializable transaction.
 */
fun interface ReservationAttemptHook {

    /**
     * Runs inside the retryable transaction before the inventory row is updated.
     */
    fun beforeUpdate(attempt: Int)
}

/**
 * Inventory table used by the CockroachDB retry workshop.
 */
object CockroachInventory: Table("bt4k_workshop_inventory") {
    val sku = varchar("sku", 64)
    val quantityOnHand = integer("quantity_on_hand")
    val version = long("version")

    override val primaryKey: PrimaryKey = PrimaryKey(sku)
}

/**
 * Ledger table used by the CockroachDB retry workshop.
 */
object CockroachLedger: Table("bt4k_workshop_ledger") {
    val id = long("id").autoIncrement()
    val sku = varchar("sku", 64)
    val quantityDelta = integer("quantity_delta")
    val reason = varchar("reason", 128)

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

/**
 * Application-facing service that reserves stock inside a CockroachDB
 * serializable transaction.
 *
 * The service deliberately delegates retry ownership to
 * [withCockroachTransaction] so non-retryable SQL errors are not replayed.
 */
class CockroachInventoryService(
    private val db: Database,
    private val retryOptions: CockroachTransactionRetryOptions = workshopRetryOptions(),
) {

    /**
     * Recreates the workshop tables and seeds a single inventory row.
     */
    fun bootstrapSchema(initialSku: String = "book-1", initialQuantity: Int = 10): InventorySnapshot {
        initialSku.requireNotBlank("initialSku")
        initialQuantity.requirePositiveNumber("initialQuantity")

        transaction(db) {
            runCatching {
                SchemaUtils.drop(CockroachLedger, CockroachInventory)
            }
            SchemaUtils.create(CockroachInventory, CockroachLedger)
            CockroachInventory.insert {
                it[sku] = initialSku
                it[quantityOnHand] = initialQuantity
                it[version] = 0L
            }
        }

        return inventory(initialSku)
    }

    /**
     * Reserves inventory and writes a matching ledger row in one retryable
     * CockroachDB transaction.
     */
    fun reserve(
        sku: String,
        quantity: Int,
        reason: String = "checkout",
        attemptHook: ReservationAttemptHook = ReservationAttemptHook {},
    ): InventorySnapshot {
        sku.requireNotBlank("sku")
        reason.requireNotBlank("reason")
        quantity.requirePositiveNumber("quantity")

        var attempt = 0
        return withCockroachTransaction(
            db = db,
            options = retryOptions,
        ) {
            attempt += 1
            attemptHook.beforeUpdate(attempt)

            val current = CockroachInventory
                .selectAll()
                .where { CockroachInventory.sku eq sku }
                .singleOrNull()
                ?.toInventorySnapshot()
                ?: error("Inventory SKU '$sku' was not bootstrapped.")

            require(current.quantityOnHand >= quantity) {
                "Insufficient inventory for SKU '$sku': requested=$quantity, onHand=${current.quantityOnHand}"
            }

            CockroachInventory.update({ CockroachInventory.sku eq sku }) {
                it[quantityOnHand] = current.quantityOnHand - quantity
                it[version] = current.version + 1
            }
            CockroachLedger.insert {
                it[CockroachLedger.sku] = sku
                it[quantityDelta] = -quantity
                it[CockroachLedger.reason] = reason
            }

            CockroachInventory
                .selectAll()
                .where { CockroachInventory.sku eq sku }
                .single()
                .toInventorySnapshot()
        }
    }

    /**
     * Reads the current inventory snapshot.
     */
    fun inventory(sku: String): InventorySnapshot {
        sku.requireNotBlank("sku")

        return transaction(db) {
            CockroachInventory
                .selectAll()
                .where { CockroachInventory.sku eq sku }
                .single()
                .toInventorySnapshot()
        }
    }

    /**
     * Counts ledger entries for the given SKU.
     */
    fun ledgerCount(sku: String): Long {
        sku.requireNotBlank("sku")

        return transaction(db) {
            CockroachLedger
                .selectAll()
                .where { CockroachLedger.sku eq sku }
                .count()
        }
    }
}

/**
 * Returns the retry policy used by the workshop tests and README examples.
 */
fun workshopRetryOptions(): CockroachTransactionRetryOptions =
    CockroachTransactionRetryOptions(
        maxAttempts = 3,
        minRetryDelay = 0.milliseconds,
        maxRetryDelay = 0.milliseconds,
        queryTimeout = 3_000.milliseconds,
        transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
    )

/**
 * Creates a deterministic CockroachDB retryable transaction exception.
 */
fun cockroachRetryableSerializationFailure(
    message: String = "restart transaction: simulated serializable conflict",
): SQLException =
    SQLException(message, "40001")

private fun ResultRow.toInventorySnapshot(): InventorySnapshot =
    InventorySnapshot(
        sku = this[CockroachInventory.sku],
        quantityOnHand = this[CockroachInventory.quantityOnHand],
        version = this[CockroachInventory.version],
    )
