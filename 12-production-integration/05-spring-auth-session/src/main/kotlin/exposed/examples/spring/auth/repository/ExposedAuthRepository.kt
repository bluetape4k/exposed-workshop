package exposed.examples.spring.auth.repository

import exposed.examples.spring.auth.model.AuthSessionRecord
import exposed.examples.spring.auth.model.AuthUser
import io.bluetape4k.codec.Base58
import jakarta.annotation.PostConstruct
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
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
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Repository

@Repository
internal class ExposedAuthRepository(
    private val database: Database,
    private val passwordEncoder: PasswordEncoder,
) : AuthRepository {
    private companion object {
        val sessionTtl: Duration = Duration.ofHours(1)
    }

    @PostConstruct
    fun initialize() {
        transaction(database) {
            SchemaUtils.create(AuthUsers, AuthUserRoles, AuthSessions)
            if (AuthUsers.selectAll().empty()) {
                seedUsers()
            }
        }
    }

    override fun findUser(username: String): AuthUser? =
        transaction(database) {
            val row = AuthUsers.selectAll()
                .where { AuthUsers.username eq username }
                .singleOrNull()
                ?: return@transaction null
            val roles = AuthUserRoles.selectAll()
                .where { AuthUserRoles.userId eq row[AuthUsers.id].value }
                .map { it[AuthUserRoles.role] }
                .toSet()
            row.toUser(roles)
        }

    override fun createSession(username: String): AuthSessionRecord =
        transaction(database) {
            val token = "spring_${Base58.randomString(24)}"
            val issuedAt = Instant.now()
            val expiresAt = issuedAt.plus(sessionTtl)
            val id = AuthSessions.insertAndGetId {
                it[AuthSessions.username] = username
                it[AuthSessions.tokenHash] = SessionTokenHasher.hash(token)
                it[issuedAtEpochMs] = issuedAt.toEpochMilli()
                it[expiresAtEpochMs] = expiresAt.toEpochMilli()
            }.value
            AuthSessionRecord(
                id = id,
                username = username,
                token = token,
                issuedAt = issuedAt,
                expiresAt = expiresAt,
            )
        }

    override fun findSessions(username: String): List<AuthSessionRecord> =
        transaction(database) {
            val nowEpochMs = Instant.now().toEpochMilli()
            AuthSessions.selectAll()
                .where {
                    (AuthSessions.username eq username) and
                        (AuthSessions.expiresAtEpochMs greater nowEpochMs)
                }
                .orderBy(AuthSessions.id to SortOrder.ASC)
                .map(::toSession)
        }

    private fun seedUsers() {
        val users = listOf(
            SeedUser("alice", "password", "Alice Reader", listOf("USER")),
            SeedUser("admin", "password", "Admin Operator", listOf("USER", "ADMIN")),
        )
        val userIds = users.associate { user ->
            val encodedPassword = requireNotNull(passwordEncoder.encode(user.password)) {
                "Password encoder returned null for ${user.username}"
            }
            val id = AuthUsers.insertAndGetId {
                it[username] = user.username
                it[passwordHash] = encodedPassword
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
            issuedAt = Instant.ofEpochMilli(row[AuthSessions.issuedAtEpochMs]),
            expiresAt = Instant.ofEpochMilli(row[AuthSessions.expiresAtEpochMs]),
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

private object AuthUsers : LongIdTable("auth_users") {
    val username = varchar("username", 80).uniqueIndex()
    val passwordHash = varchar("password_hash", 120)
    val displayName = varchar("display_name", 120)
}

private object AuthUserRoles : Table("auth_user_roles") {
    val userId = long("user_id").references(AuthUsers.id)
    val role = varchar("role", 40)
    override val primaryKey = PrimaryKey(userId, role)
}

private object AuthSessions : LongIdTable("auth_sessions") {
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
