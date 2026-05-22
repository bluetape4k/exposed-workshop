package exposed.examples.ktor.auth.service

import exposed.examples.ktor.auth.model.AuthSessionRecord
import exposed.examples.ktor.auth.model.AuthUser
import exposed.examples.ktor.auth.model.PermissionDeniedException
import exposed.examples.ktor.auth.repository.AuthRepository
import java.io.Serializable
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

internal class AuthService(
    private val repository: AuthRepository,
) {
    suspend fun authenticate(username: String, password: String): AuthUser? {
        val user = repository.findUser(username) ?: return null
        return user.takeIf { PasswordHasher.matches(password, it.passwordHash) }
    }

    suspend fun profile(username: String): AuthUser =
        repository.findUser(username)
            ?: throw NoSuchElementException("User $username was not found")

    suspend fun adminProfile(username: String): AuthUser {
        val user = profile(username)
        if ("ADMIN" !in user.roles) {
            throw PermissionDeniedException("ADMIN role is required")
        }
        return user
    }

    suspend fun createSession(username: String): AuthSessionRecord =
        repository.createSession(username)

    suspend fun profileBySessionToken(token: String): AuthUser? {
        val session = repository.findSessionByToken(token)
            ?: return null
        return profile(session.username)
    }

    suspend fun sessions(username: String): List<AuthSessionRecord> =
        repository.findSessions(username)
}

internal data class AuthPrincipal(
    val username: String,
    val roles: Set<String>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal object PasswordHasher {
    private val encoder = BCryptPasswordEncoder()

    fun hash(password: String): String =
        checkNotNull(encoder.encode(password)) {
            "BCrypt encoder returned null"
        }

    fun matches(password: String, passwordHash: String): Boolean =
        encoder.matches(password, passwordHash)
}
