package exposed.examples.spring.realtime.model

import java.io.Serializable

internal data class IndexResponse(
    val service: String = "spring-outbox-realtime",
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 5601475034471714701L
    }
}

internal data class HealthResponse(
    val status: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 5870639498496029410L
    }
}

internal data class CreateNotificationRequest(
    val recipientId: String,
    val message: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1581867208748892395L
    }
}

internal data class NotificationResponse(
    val eventId: Long,
    val recipientId: String,
    val message: String,
    val status: OutboxStatus,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = -6286082907388769034L
    }
}

internal data class OutboxResponse(
    val events: List<NotificationResponse>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = -6298862713510368084L
    }
}

internal data class PublishResponse(
    val attempted: Int,
    val delivered: Int,
    val failed: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 4309189964493080058L
    }
}

internal data class CreateNotificationCommand(
    val recipientId: String,
    val message: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 6942460571229875806L
    }
}

internal data class RealtimeEvent(
    val id: Long,
    val recipientId: String,
    val message: String,
    val type: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2824956714925506161L
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
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = -1636886299458813869L
    }
}

internal data class PublishSummary(
    val attempted: Int,
    val delivered: Int,
    val failed: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = -8975708743154064278L
    }
}

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
