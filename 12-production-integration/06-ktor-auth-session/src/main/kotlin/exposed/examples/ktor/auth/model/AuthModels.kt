package exposed.examples.ktor.auth.model

import java.io.Serializable
import kotlinx.serialization.Serializable as KSerializable

@KSerializable
internal data class IndexResponse(
    val service: String = "ktor-auth-session",
    val description: String = "Ktor Authentication and Sessions backed by Exposed",
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@KSerializable
internal data class HealthResponse(
    val status: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class AuthUser(
    val id: Long,
    val username: String,
    val passwordHash: String,
    val displayName: String,
    val roles: Set<String>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class AuthSessionRecord(
    val id: Long,
    val username: String,
    val token: String?,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@KSerializable
internal data class ProfileResponse(
    val username: String,
    val displayName: String,
    val roles: Set<String>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@KSerializable
internal data class SessionResponse(
    val token: String?,
    val username: String,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@KSerializable
internal data class SessionsResponse(
    val sessions: List<SessionResponse>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@KSerializable
internal data class ErrorResponse(
    val code: String,
    val message: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@KSerializable
internal data class AuthSessionCookie(
    val token: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal class PermissionDeniedException(message: String) : RuntimeException(message)

internal fun AuthUser.toProfileResponse(): ProfileResponse =
    ProfileResponse(
        username = username,
        displayName = displayName,
        roles = roles,
    )

internal fun AuthSessionRecord.toResponse(): SessionResponse =
    SessionResponse(
        token = token,
        username = username,
        issuedAtEpochMs = issuedAtEpochMs,
        expiresAtEpochMs = expiresAtEpochMs,
    )
