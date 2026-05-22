package exposed.examples.ktor.httpoutbox.repository

import exposed.examples.ktor.httpoutbox.model.CreatePaymentCommand
import exposed.examples.ktor.httpoutbox.model.CreatePaymentResult
import exposed.examples.ktor.httpoutbox.model.PaymentRecord
import exposed.examples.ktor.httpoutbox.model.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.concurrent.atomic.AtomicBoolean

internal class ExposedPaymentOutboxRepository(
    private val database: Database,
) : PaymentOutboxRepository {

    private val schemaReady = AtomicBoolean(false)
    private val schemaMutex = Mutex()

    override suspend fun createPending(command: CreatePaymentCommand): CreatePaymentResult {
        ensureSchema()
        return try {
            transactionIO {
                findByIdempotencyKey(command.idempotencyKey)?.let {
                    CreatePaymentResult(it, inserted = false)
                } ?: insertPending(command)
            }
        } catch (e: ExposedSQLException) {
            rereadDuplicate(command.idempotencyKey) ?: throw e
        }
    }

    override suspend fun findById(id: Long): PaymentRecord? {
        ensureSchema()
        return transactionIO {
            findByIdInTransaction(id)
        }
    }

    override suspend fun findAll(): List<PaymentRecord> {
        ensureSchema()
        return transactionIO {
            PaymentOutbox.selectAll().map { it.toRecord() }
        }
    }

    override suspend fun markSucceeded(id: Long, externalId: String): PaymentRecord =
        updateStatus(id, PaymentStatus.SUCCEEDED, externalId, null)

    override suspend fun markRetryableFailure(id: Long, message: String): PaymentRecord =
        updateStatus(id, PaymentStatus.RETRYABLE_FAILED, null, message)

    override suspend fun markPermanentFailure(id: Long, message: String): PaymentRecord =
        updateStatus(id, PaymentStatus.PERMANENT_FAILED, null, message)

    override suspend fun deleteAll() {
        ensureSchema()
        transactionIO {
            PaymentOutbox.deleteAll()
        }
    }

    private suspend fun updateStatus(
        id: Long,
        nextStatus: PaymentStatus,
        externalId: String?,
        lastError: String?,
    ): PaymentRecord {
        ensureSchema()
        return transactionIO {
            val current = findByIdInTransaction(id)
                ?: throw NoSuchElementException("Payment request $id was not found")
            PaymentOutbox.update({ PaymentOutbox.id eq id }) {
                it[status] = nextStatus.name
                it[attempts] = current.attempts + 1
                it[PaymentOutbox.externalId] = externalId
                it[PaymentOutbox.lastError] = lastError
            }
            checkNotNull(findByIdInTransaction(id)) {
                "Updated payment request $id was not found"
            }
        }
    }

    private suspend fun ensureSchema() {
        if (schemaReady.get()) {
            return
        }
        schemaMutex.withLock {
            if (!schemaReady.get()) {
                transactionIO {
                    SchemaUtils.create(PaymentOutbox)
                }
                schemaReady.set(true)
            }
        }
    }

    private suspend fun <T> transactionIO(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction(database) {
                block()
            }
        }

    private fun findByIdInTransaction(id: Long): PaymentRecord? =
        PaymentOutbox.selectAll()
            .where { PaymentOutbox.id eq id }
            .singleOrNull()
            ?.toRecord()

    private fun findByIdempotencyKey(idempotencyKey: String): PaymentRecord? =
        PaymentOutbox.selectAll()
            .where { PaymentOutbox.idempotencyKey eq idempotencyKey }
            .singleOrNull()
            ?.toRecord()

    private fun insertPending(command: CreatePaymentCommand): CreatePaymentResult {
        val id = PaymentOutbox.insertAndGetId {
            it[orderId] = command.orderId
            it[amountCents] = command.amountCents
            it[idempotencyKey] = command.idempotencyKey
            it[status] = PaymentStatus.PENDING.name
            it[attempts] = 0
        }.value
        return CreatePaymentResult(
            checkNotNull(findByIdInTransaction(id)) {
                "Inserted payment request $id was not found"
            },
            inserted = true
        )
    }

    private suspend fun rereadDuplicate(idempotencyKey: String): CreatePaymentResult? =
        transactionIO {
            findByIdempotencyKey(idempotencyKey)?.let {
                CreatePaymentResult(it, inserted = false)
            }
        }
}

private object PaymentOutbox : LongIdTable("ktor_payment_outbox") {
    val orderId = varchar("order_id", 80)
    val amountCents = long("amount_cents")
    val idempotencyKey = varchar("idempotency_key", 120).uniqueIndex()
    val status = varchar("status", 32)
    val attempts = integer("attempts")
    val externalId = varchar("external_id", 120).nullable()
    val lastError = varchar("last_error", 240).nullable()
}

private fun ResultRow.toRecord(): PaymentRecord =
    PaymentRecord(
        id = this[PaymentOutbox.id].value,
        orderId = this[PaymentOutbox.orderId],
        amountCents = this[PaymentOutbox.amountCents],
        idempotencyKey = this[PaymentOutbox.idempotencyKey],
        status = PaymentStatus.valueOf(this[PaymentOutbox.status]),
        attempts = this[PaymentOutbox.attempts],
        externalId = this[PaymentOutbox.externalId],
        lastError = this[PaymentOutbox.lastError]
    )
