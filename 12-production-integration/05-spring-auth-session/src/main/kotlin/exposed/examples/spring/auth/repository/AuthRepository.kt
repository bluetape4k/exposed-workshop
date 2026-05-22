package exposed.examples.spring.auth.repository

import exposed.examples.spring.auth.model.AuthSessionRecord
import exposed.examples.spring.auth.model.AuthUser

internal interface AuthRepository {
    fun findUser(username: String): AuthUser?
    fun createSession(username: String): AuthSessionRecord
    fun findSessions(username: String): List<AuthSessionRecord>
}
