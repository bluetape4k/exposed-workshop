package exposed.examples.ktor.auth.repository

import exposed.examples.ktor.auth.model.AuthSessionRecord
import exposed.examples.ktor.auth.model.AuthUser
import exposed.examples.ktor.auth.service.PasswordHasher
import io.bluetape4k.codec.Base58
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal class ExposedAuthRepository(
    private val database: Database,
) : AuthRepository {
    private companion object {
        val sessionTtl: Duration = Duration.ofHours(1)
    }

    private val schemaReady = AtomicBoolean(false)
    private val schemaMutex = Mutex()

    override suspend fun findUser(username: String): AuthUser? {
        ensureSchema()
        return transactionIO {
            val row = AuthUsers.selectAll()
                .where { AuthUsers.username eq username }
                .singleOrNull()
                ?: return@transactionIO null
            val roles = AuthUserRoles.selectAll()
                .where { AuthUserRoles.userId eq row[AuthUsers.id].value }
                .map { it[AuthUserRoles.role] }
                .toSet()
            row.toUser(roles)
        }
    }

    override suspend fun createSession(username: String): AuthSessionRecord {
        ensureSchema()
        return transactionIO {
            val issuedAtEpochMs = Instant.now().toEpochMilli()
            val expiresAtEpochMs = issuedAtEpochMs + sessionTtl.toMillis()
            val token = "ktor_${Base58.randomString(24)}"
            val id = AuthSessions.insertAndGetId {
                it[AuthSessions.username] = username
                it[AuthSessions.tokenHash] = SessionTokenHasher.hash(token)
                it[AuthSessions.issuedAtEpochMs] = issuedAtEpochMs
                it[AuthSessions.expiresAtEpochMs] = expiresAtEpochMs
            }.value
            AuthSessionRecord(
                id = id,
                username = username,
                token = token,
                issuedAtEpochMs = issuedAtEpochMs,
                expiresAtEpochMs = expiresAtEpochMs,
            )
        }
    }

    override suspend fun findSessionByToken(token: String): AuthSessionRecord? {
        ensureSchema()
        val tokenHash = SessionTokenHasher.hash(token)
        val nowEpochMs = Instant.now().toEpochMilli()
        return transactionIO {
            AuthSessions.selectAll()
                .where {
                    (AuthSessions.tokenHash eq tokenHash) and
                        (AuthSessions.expiresAtEpochMs greater nowEpochMs)
                }
                .singleOrNull()
                ?.let(::toSession)
        }
    }

    override suspend fun findSessions(username: String): List<AuthSessionRecord> {
        ensureSchema()
        val nowEpochMs = Instant.now().toEpochMilli()
        return transactionIO {
            AuthSessions.selectAll()
                .where {
                    (AuthSessions.username eq username) and
                        (AuthSessions.expiresAtEpochMs greater nowEpochMs)
                }
                .orderBy(AuthSessions.id to SortOrder.ASC)
                .map(::toSession)
        }
    }

    override suspend fun deleteAllSessions() {
        ensureSchema()
        transactionIO {
            AuthSessions.deleteAll()
        }
    }

    private suspend fun ensureSchema() {
        if (schemaReady.get()) {
            return
        }
        schemaMutex.withLock {
            if (!schemaReady.get()) {
                transactionIO {
                    SchemaUtils.create(AuthUsers, AuthUserRoles, AuthSessions)
                    if (AuthUsers.selectAll().empty()) {
                        seedUsers()
                    }
                }
                schemaReady.set(true)
            }
        }
    }

    private suspend fun <T> transactionIO(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction(database) {
                block()
            }
        }

    private fun seedUsers() {
        val users = listOf(
            SeedUser("alice", "password", "Alice Reader", listOf("USER")),
            SeedUser("admin", "password", "Admin Operator", listOf("USER", "ADMIN")),
        )
        val userIds = users.associate { user ->
            val id = AuthUsers.insertAndGetId {
                it[username] = user.username
                it[passwordHash] = PasswordHasher.hash(user.password)
                it[displayName] = user.displayName
            }.value
            user.username to id
        }
        val roleRows = users.flatMap { user ->
            user.roles.map { role -> SeedUserRole(user.username, role) }
        }
        AuthUserRoles.batchInsert(roleRows) { row ->
            this[AuthUserRoles.userId] = userIds.getValue(row.username)
            this[AuthUserRoles.role] = row.role
        }
    }

    private fun ResultRow.toUser(roles: Set<String>): AuthUser =
        AuthUser(
            id = this[AuthUsers.id].value,
            username = this[AuthUsers.username],
            passwordHash = this[AuthUsers.passwordHash],
            displayName = this[AuthUsers.displayName],
            roles = roles,
        )

    private fun toSession(row: ResultRow): AuthSessionRecord =
        AuthSessionRecord(
            id = row[AuthSessions.id].value,
            username = row[AuthSessions.username],
            token = null,
            issuedAtEpochMs = row[AuthSessions.issuedAtEpochMs],
            expiresAtEpochMs = row[AuthSessions.expiresAtEpochMs],
        )

    private data class SeedUser(
        val username: String,
        val password: String,
        val displayName: String,
        val roles: List<String>,
    )

    private data class SeedUserRole(
        val username: String,
        val role: String,
    )
}

private object AuthUsers : LongIdTable("ktor_auth_users") {
    val username = varchar("username", 80).uniqueIndex()
    val passwordHash = varchar("password_hash", 120)
    val displayName = varchar("display_name", 120)
}

private object AuthUserRoles : Table("ktor_auth_user_roles") {
    val userId = long("user_id").references(AuthUsers.id)
    val role = varchar("role", 40)
    override val primaryKey = PrimaryKey(userId, role)
}

private object AuthSessions : LongIdTable("ktor_auth_sessions") {
    val username = varchar("username", 80)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val issuedAtEpochMs = long("issued_at_epoch_ms")
    val expiresAtEpochMs = long("expires_at_epoch_ms")
}

private object SessionTokenHasher {
    fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
