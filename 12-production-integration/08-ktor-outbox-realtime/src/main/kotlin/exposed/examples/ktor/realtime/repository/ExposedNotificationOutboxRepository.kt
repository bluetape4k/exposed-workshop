package exposed.examples.ktor.realtime.repository

import exposed.examples.ktor.realtime.model.CreateNotificationCommand
import exposed.examples.ktor.realtime.model.OutboxStatus
import exposed.examples.ktor.realtime.model.RealtimeEventRecord
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ExposedNotificationOutboxRepository(
    private val database: Database,
) : NotificationOutboxRepository {

    private val schemaReady = AtomicBoolean(false)

    override fun createPending(command: CreateNotificationCommand): RealtimeEventRecord {
        ensureSchema()
        return transaction(database) {
            val notificationId = Notifications.insertAndGetId {
                it[recipientId] = command.recipientId
                it[message] = command.message
            }.value
            val eventId = RealtimeOutbox.insertAndGetId {
                it[aggregateId] = notificationId
                it[recipientId] = command.recipientId
                it[eventType] = "notification.created"
                it[payload] = command.message
                it[status] = OutboxStatus.PENDING.name
                it[attempts] = 0
            }.value
            checkNotNull(findByIdInTransaction(eventId)) {
                "Inserted realtime outbox event $eventId was not found"
            }
        }
    }

    override fun findAll(): List<RealtimeEventRecord> {
        ensureSchema()
        return transaction(database) {
            RealtimeOutbox.selectAll()
                .orderBy(RealtimeOutbox.id to SortOrder.ASC)
                .map { it.toRecord() }
        }
    }

    override fun findPending(): List<RealtimeEventRecord> {
        ensureSchema()
        return transaction(database) {
            RealtimeOutbox.selectAll()
                .where { RealtimeOutbox.status eq OutboxStatus.PENDING.name }
                .orderBy(RealtimeOutbox.id to SortOrder.ASC)
                .map { it.toRecord() }
        }
    }

    override fun replayAfter(eventId: Long): List<RealtimeEventRecord> {
        ensureSchema()
        return transaction(database) {
            RealtimeOutbox.selectAll()
                .where { RealtimeOutbox.status eq OutboxStatus.PUBLISHED.name }
                .orderBy(RealtimeOutbox.id to SortOrder.ASC)
                .map { it.toRecord() }
                .filter { it.id > eventId }
        }
    }

    override fun markPublished(eventId: Long): RealtimeEventRecord =
        updateStatus(eventId, OutboxStatus.PUBLISHED, lastError = null)

    override fun markFailed(eventId: Long, message: String): RealtimeEventRecord =
        updateStatus(eventId, OutboxStatus.FAILED, lastError = message.take(MAX_ERROR_LENGTH))

    override fun deleteAll() {
        ensureSchema()
        transaction(database) {
            RealtimeOutbox.deleteAll()
            Notifications.deleteAll()
        }
    }

    private fun updateStatus(
        eventId: Long,
        nextStatus: OutboxStatus,
        lastError: String?,
    ): RealtimeEventRecord {
        ensureSchema()
        return transaction(database) {
            val current = findByIdInTransaction(eventId)
                ?: throw NoSuchElementException("Realtime outbox event $eventId was not found")
            RealtimeOutbox.update({ RealtimeOutbox.id eq eventId }) {
                it[status] = nextStatus.name
                it[attempts] = current.attempts + 1
                it[RealtimeOutbox.lastError] = lastError
            }
            checkNotNull(findByIdInTransaction(eventId)) {
                "Updated realtime outbox event $eventId was not found"
            }
        }
    }

    private fun ensureSchema() {
        if (schemaReady.get()) {
            return
        }
        schemaLock.withLock {
            if (schemaReady.compareAndSet(false, true)) {
                transaction(database) {
                    SchemaUtils.create(Notifications, RealtimeOutbox)
                }
            }
        }
    }

    private fun findByIdInTransaction(eventId: Long): RealtimeEventRecord? =
        RealtimeOutbox.selectAll()
            .where { RealtimeOutbox.id eq eventId }
            .singleOrNull()
            ?.toRecord()
}

private const val MAX_ERROR_LENGTH = 240

private val schemaLock = ReentrantLock()

private object Notifications : LongIdTable("ktor_realtime_notifications") {
    val recipientId = varchar("recipient_id", 80)
    val message = varchar("message", 240)
}

private object RealtimeOutbox : LongIdTable("ktor_realtime_outbox") {
    val aggregateId = long("aggregate_id")
    val recipientId = varchar("recipient_id", 80)
    val eventType = varchar("event_type", 80)
    val payload = varchar("payload", 240)
    val status = varchar("status", 32)
    val attempts = integer("attempts")
    val lastError = varchar("last_error", 240).nullable()
}

private fun ResultRow.toRecord(): RealtimeEventRecord =
    RealtimeEventRecord(
        id = this[RealtimeOutbox.id].value,
        recipientId = this[RealtimeOutbox.recipientId],
        message = this[RealtimeOutbox.payload],
        type = this[RealtimeOutbox.eventType],
        status = OutboxStatus.valueOf(this[RealtimeOutbox.status]),
        attempts = this[RealtimeOutbox.attempts],
        lastError = this[RealtimeOutbox.lastError]
    )
