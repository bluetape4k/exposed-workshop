package exposed.examples.spring.httpoutbox.model

import java.io.Serializable

data class IndexResponse(
    val service: String = "spring-http-outbox-idempotency",
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class HealthResponse(
    val status: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class CreatePaymentRequest(
    val orderId: String = "",
    val amountCents: Long = 0,
    val idempotencyKey: String = "",
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class CreatePaymentCommand(
    val orderId: String,
    val amountCents: Long,
    val idempotencyKey: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

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
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class CreatePaymentResult(
    val record: PaymentRecord,
    val inserted: Boolean,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

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
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class PaymentsResponse(
    val payments: List<PaymentResponse>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class ExternalPaymentRequest(
    val orderId: String,
    val amountCents: Long,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class ExternalPaymentResponse(
    val externalId: String = "",
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class ExternalPaymentResult(
    val externalId: String,
) : Serializable {
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
