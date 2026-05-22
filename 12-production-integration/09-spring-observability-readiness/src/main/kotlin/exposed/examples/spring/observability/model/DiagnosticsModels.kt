package exposed.examples.spring.observability.model

import java.io.Serializable
import java.time.Instant

internal data class DiagnosticRecord(
    val id: Long,
    val name: String,
    val requestId: String,
    val durationMs: Long,
    val slow: Boolean,
    val createdAt: Instant,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class OperationDiagnosticsResponse(
    val id: Long,
    val name: String,
    val requestId: String,
    val durationMs: Long,
    val slow: Boolean,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class OperationsResponse(
    val operations: List<OperationDiagnosticsResponse>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class ErrorResponse(
    val code: String,
    val message: String,
    val requestId: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal fun DiagnosticRecord.toResponse(): OperationDiagnosticsResponse =
    OperationDiagnosticsResponse(
        id = id,
        name = name,
        requestId = requestId,
        durationMs = durationMs,
        slow = slow,
    )
