package exposed.examples.ktor.httpoutbox.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class IndexResponse(
    val service: String = "ktor-http-outbox-idempotency",
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class HealthResponse(
    val status: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class CreatePaymentRequest(
    val orderId: String = "",
    val amountCents: Long = 0,
    val idempotencyKey: String = "",
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class CreatePaymentCommand(
    val orderId: String,
    val amountCents: Long,
    val idempotencyKey: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
enum class PaymentStatus {
    PENDING,
    SUCCEEDED,
    RETRYABLE_FAILED,
    PERMANENT_FAILED,
}

internal data class PaymentRecord(
    val id: Long,
    val orderId: String,
    val amountCents: Long,
    val idempotencyKey: String,
    val status: PaymentStatus,
    val attempts: Int,
    val externalId: String?,
    val lastError: String?,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class CreatePaymentResult(
    val record: PaymentRecord,
    val inserted: Boolean,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class PaymentResponse(
    val id: Long,
    val orderId: String,
    val amountCents: Long,
    val idempotencyKey: String,
    val status: PaymentStatus,
    val attempts: Int,
    val externalId: String?,
    val lastError: String?,
    val duplicate: Boolean,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class PaymentsResponse(
    val payments: List<PaymentResponse>,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class ExternalPaymentRequest(
    val orderId: String,
    val amountCents: Long,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class ExternalPaymentResponse(
    val externalId: String = "",
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class ExternalPaymentResult(
    val externalId: String,
) : JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal class PaymentValidationException(message: String) : RuntimeException(message)

internal open class ExternalPaymentException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

internal class RetryableExternalPaymentException(
    message: String,
    cause: Throwable? = null,
) : ExternalPaymentException(message, cause)

internal class PermanentExternalPaymentException(
    message: String,
    cause: Throwable? = null,
) : ExternalPaymentException(message, cause)

internal fun PaymentRecord.toResponse(duplicate: Boolean = false): PaymentResponse =
    PaymentResponse(
        id = id,
        orderId = orderId,
        amountCents = amountCents,
        idempotencyKey = idempotencyKey,
        status = status,
        attempts = attempts,
        externalId = externalId,
        lastError = lastError,
        duplicate = duplicate
    )
