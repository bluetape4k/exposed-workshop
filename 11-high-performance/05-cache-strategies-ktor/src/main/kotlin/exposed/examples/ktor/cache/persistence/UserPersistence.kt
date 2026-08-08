package exposed.examples.ktor.cache.persistence

import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Serializable

object Users: IdTable<String>("users") {
    override val id: Column<EntityID<String>> = varchar("id", 40).entityId()
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
                    this[Users.id] = EntityID(user.id, Users)
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
