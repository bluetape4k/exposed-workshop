package exposed.examples.ktor.auth.repository

import exposed.examples.ktor.auth.model.AuthSessionRecord
import exposed.examples.ktor.auth.model.AuthUser

internal interface AuthRepository {
    suspend fun findUser(username: String): AuthUser?

    suspend fun createSession(username: String): AuthSessionRecord

    suspend fun findSessionByToken(token: String): AuthSessionRecord?

    suspend fun findSessions(username: String): List<AuthSessionRecord>

    suspend fun deleteAllSessions()
}
