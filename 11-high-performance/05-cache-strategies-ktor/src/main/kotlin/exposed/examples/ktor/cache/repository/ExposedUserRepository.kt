package exposed.examples.ktor.cache.repository

import exposed.examples.ktor.cache.model.UserResponse
import exposed.examples.ktor.cache.persistence.Users
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.concurrent.atomic.AtomicInteger

class ExposedUserRepository(
    private val database: Database,
) {
    private val readCounter = AtomicInteger()

    val databaseReads: Int
        get() = readCounter.get()

    fun find(id: String): UserResponse? {
        readCounter.incrementAndGet()
        return transaction(database) {
            Users
                .selectAll()
                .where { Users.id eq id }
                .singleOrNull()
                ?.let {
                    UserResponse(
                        id = it[Users.id],
                        displayName = it[Users.displayName],
                        version = it[Users.version],
                        source = "database",
                    )
                }
        }
    }

    fun upsert(id: String, displayName: String): UserResponse =
        transaction(database) {
            val nextVersion = (findCurrentVersion(id) ?: 0) + 1
            Users.upsert {
                it[Users.id] = id
                it[Users.displayName] = displayName
                it[Users.version] = nextVersion
            }
            UserResponse(
                id = id,
                displayName = displayName,
                version = nextVersion,
                source = "database",
            )
        }

    private fun findCurrentVersion(id: String): Int? =
        Users
            .selectAll()
            .where { Users.id eq id }
            .singleOrNull()
            ?.get(Users.version)
}
