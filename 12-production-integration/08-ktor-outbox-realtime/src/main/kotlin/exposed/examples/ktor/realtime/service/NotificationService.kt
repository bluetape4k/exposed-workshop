package exposed.examples.ktor.realtime.service

import exposed.examples.ktor.realtime.model.CreateNotificationCommand
import exposed.examples.ktor.realtime.model.CreateNotificationRequest
import exposed.examples.ktor.realtime.model.NotificationResponse
import exposed.examples.ktor.realtime.model.OutboxResponse
import exposed.examples.ktor.realtime.model.PublishResponse
import exposed.examples.ktor.realtime.model.PublishSummary
import exposed.examples.ktor.realtime.model.RealtimeEvent
import exposed.examples.ktor.realtime.model.toEvent
import exposed.examples.ktor.realtime.model.toResponse
import exposed.examples.ktor.realtime.repository.NotificationOutboxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal interface RealtimeDelivery {
    fun deliver(event: RealtimeEvent): Boolean
}

internal class RealtimeHub : RealtimeDelivery {

    private val events = MutableSharedFlow<RealtimeEvent>(
        replay = LIVE_REPLAY_CAPACITY,
        extraBufferCapacity = LIVE_BUFFER_CAPACITY
    )

    override fun deliver(event: RealtimeEvent): Boolean =
        events.tryEmit(event)

    fun live(): Flow<RealtimeEvent> =
        events

    private companion object {
        private const val LIVE_REPLAY_CAPACITY = 64
        private const val LIVE_BUFFER_CAPACITY = 64
    }
}

internal class NotificationService(
    private val repository: NotificationOutboxRepository,
    private val realtimeDelivery: RealtimeDelivery,
) {

    fun create(request: CreateNotificationRequest): NotificationResponse {
        val command = CreateNotificationCommand(
            recipientId = request.recipientId.normalize("recipientId", 80),
            message = request.message.normalize("message", 240)
        )
        return repository.createPending(command).toResponse()
    }

    fun publishPending(): PublishResponse {
        val summary = repository.findPending()
            .fold(PublishSummary(attempted = 0, delivered = 0, failed = 0)) { current, record ->
                val delivered = realtimeDelivery.deliver(record.toEvent())
                if (delivered) {
                    repository.markPublished(record.id)
                    current.copy(
                        attempted = current.attempted + 1,
                        delivered = current.delivered + 1
                    )
                } else {
                    repository.markFailed(record.id, "realtime delivery returned false")
                    current.copy(
                        attempted = current.attempted + 1,
                        failed = current.failed + 1
                    )
                }
            }
        return PublishResponse(
            attempted = summary.attempted,
            delivered = summary.delivered,
            failed = summary.failed
        )
    }

    fun replayAfter(eventId: Long): List<RealtimeEvent> =
        repository.replayAfter(eventId).map { it.toEvent() }

    fun outbox(): OutboxResponse =
        OutboxResponse(repository.findAll().map { it.toResponse() })

    fun deleteAll() {
        repository.deleteAll()
    }

    private fun String.normalize(field: String, maxLength: Int): String {
        val value = trim()
        require(value.isNotEmpty()) {
            "$field must not be blank"
        }
        require(value.length <= maxLength) {
            "$field must be at most $maxLength characters"
        }
        return value
    }
}
