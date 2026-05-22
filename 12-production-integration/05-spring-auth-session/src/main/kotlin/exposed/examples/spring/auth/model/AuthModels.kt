package exposed.examples.spring.auth.model

import java.io.Serializable
import java.time.Instant

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
    val issuedAt: Instant,
    val expiresAt: Instant,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class ProfileResponse(
    val username: String,
    val displayName: String,
    val roles: Set<String>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class SessionResponse(
    val token: String?,
    val username: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class SessionsResponse(
    val sessions: List<SessionResponse>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

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
        issuedAt = issuedAt,
        expiresAt = expiresAt,
    )
