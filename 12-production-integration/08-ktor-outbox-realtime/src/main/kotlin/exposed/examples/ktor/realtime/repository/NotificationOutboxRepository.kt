package exposed.examples.ktor.realtime.repository

import exposed.examples.ktor.realtime.model.CreateNotificationCommand
import exposed.examples.ktor.realtime.model.RealtimeEventRecord

internal interface NotificationOutboxRepository {

    fun createPending(command: CreateNotificationCommand): RealtimeEventRecord

    fun findAll(): List<RealtimeEventRecord>

    fun findPending(): List<RealtimeEventRecord>

    fun replayAfter(eventId: Long): List<RealtimeEventRecord>

    fun markPublished(eventId: Long): RealtimeEventRecord

    fun markFailed(eventId: Long, message: String): RealtimeEventRecord

    fun deleteAll()
}
