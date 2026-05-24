package exposed.examples.ktor.cache.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Serializable

object Users: Table("users") {
    val id = varchar("id", 40)
    val displayName = varchar("display_name", 120)
    val version = integer("version")

    override val primaryKey = PrimaryKey(id)
}

class UserPersistence(
    private val database: Database,
) {
    fun initialize() {
        transaction(database) {
            SchemaUtils.create(Users)
            if (Users.selectAll().empty()) {
                Users.batchInsert(
                    listOf(
                        SeedUser("u1", "Ada Lovelace", 1),
                        SeedUser("u2", "Grace Hopper", 1),
                    ),
                ) { user ->
                    this[Users.id] = user.id
                    this[Users.displayName] = user.displayName
                    this[Users.version] = user.version
                }
            }
        }
    }

    private data class SeedUser(
        val id: String,
        val displayName: String,
        val version: Int,
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
