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
 * CockroachDB 재시도 워크숍이 반환하는 재고 행이다.
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
 * CockroachDB가 클라이언트에 전체 serializable 트랜잭션 재시도를 요청하는 방식을 보여 주기 위한
 * 테스트 가능한 hook이다.
 */
fun interface ReservationAttemptHook {

    /**
     * 재고 행을 갱신하기 전에 재시도 가능한 트랜잭션 내부에서 실행된다.
     */
    fun beforeUpdate(attempt: Int)
}

/**
 * CockroachDB 재시도 워크숍에서 사용하는 재고 테이블이다.
 */
object CockroachInventory: Table("bt4k_workshop_inventory") {
    val sku = varchar("sku", 64)
    val quantityOnHand = integer("quantity_on_hand")
    val version = long("version")

    override val primaryKey: PrimaryKey = PrimaryKey(sku)
}

/**
 * CockroachDB 재시도 워크숍에서 사용하는 ledger 테이블이다.
 */
object CockroachLedger: Table("bt4k_workshop_ledger") {
    val id = long("id").autoIncrement()
    val sku = varchar("sku", 64)
    val quantityDelta = integer("quantity_delta")
    val reason = varchar("reason", 128)

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

/**
 * 애플리케이션에서 호출하는 서비스이며 CockroachDB
 * serializable 트랜잭션 안에서 재고를 예약한다.
 *
 * 이 서비스는 재시도 책임을 의도적으로
 * [withCockroachTransaction]에 위임해서 재시도 불가능한 SQL 오류를 반복 실행하지 않는다.
 */
class CockroachInventoryService(
    private val db: Database,
    private val retryOptions: CockroachTransactionRetryOptions = workshopRetryOptions(),
) {

    /**
     * 워크숍 테이블을 다시 만들고 단일 재고 행을 시드한다.
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
     * 하나의 재시도 가능한
     * CockroachDB 트랜잭션에서 재고를 예약하고 대응되는 ledger 행을 기록한다.
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
     * 현재 재고 스냅숏을 읽는다.
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
     * 지정한 SKU의 ledger 항목 수를 계산한다.
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
 * 워크숍 테스트와 README 예제에서 사용하는 재시도 정책을 반환한다.
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
 * 결정적인 CockroachDB 재시도 가능 트랜잭션 예외를 생성한다.
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
