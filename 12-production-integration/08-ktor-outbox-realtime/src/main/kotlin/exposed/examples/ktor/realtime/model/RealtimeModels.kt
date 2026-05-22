package exposed.examples.ktor.realtime.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JvmSerializable

@Serializable
internal data class IndexResponse(
    val service: String = "ktor-outbox-realtime",
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = 262014258189345253L
    }
}

@Serializable
internal data class HealthResponse(
    val status: String,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = 4439067661518748401L
    }
}

@Serializable
internal data class ErrorResponse(
    val error: String,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = -8077922284710325423L
    }
}

@Serializable
internal data class CreateNotificationRequest(
    val recipientId: String,
    val message: String,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = 8143961982690564487L
    }
}

@Serializable
internal data class NotificationResponse(
    val eventId: Long,
    val recipientId: String,
    val message: String,
    val status: OutboxStatus,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = 6845909320151762289L
    }
}

@Serializable
internal data class OutboxResponse(
    val events: List<NotificationResponse>,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = 3099750876760237277L
    }
}

@Serializable
internal data class PublishResponse(
    val attempted: Int,
    val delivered: Int,
    val failed: Int,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = -8413461889801968240L
    }
}

internal data class CreateNotificationCommand(
    val recipientId: String,
    val message: String,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = -4878785294177805372L
    }
}

@Serializable
internal data class RealtimeEvent(
    val id: Long,
    val recipientId: String,
    val message: String,
    val type: String,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = -3412694985695299617L
    }
}

internal data class RealtimeEventRecord(
    val id: Long,
    val recipientId: String,
    val message: String,
    val type: String,
    val status: OutboxStatus,
    val attempts: Int,
    val lastError: String?,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = 1977467623189607112L
    }
}

internal data class PublishSummary(
    val attempted: Int,
    val delivered: Int,
    val failed: Int,
) : JvmSerializable {
    companion object {
        private const val serialVersionUID: Long = 8003156459604997722L
    }
}

@Serializable
internal enum class OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}

internal fun RealtimeEventRecord.toEvent(): RealtimeEvent =
    RealtimeEvent(
        id = id,
        recipientId = recipientId,
        message = message,
        type = type
    )

internal fun RealtimeEventRecord.toResponse(): NotificationResponse =
    NotificationResponse(
        eventId = id,
        recipientId = recipientId,
        message = message,
        status = status
    )
