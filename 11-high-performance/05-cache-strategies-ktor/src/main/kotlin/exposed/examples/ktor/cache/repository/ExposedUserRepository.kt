package exposed.examples.ktor.cache.repository

import exposed.examples.ktor.cache.model.UserResponse
import exposed.examples.ktor.cache.persistence.Users
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.jdbc.caffeine.repository.AbstractJdbcCaffeineRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.concurrent.atomic.AtomicInteger

class ExposedUserRepository(
    config: LocalCacheConfig = LocalCacheConfig(
        keyPrefix = "ktor-users",
        maximumSize = 1_000,
        writeMode = CacheWriteMode.WRITE_THROUGH,
    ),
    private val assertOwner: () -> Unit = {},
) : AbstractJdbcCaffeineRepository<String, UserResponse>(config) {

    private val readCounter = AtomicInteger()

    override val table: IdTable<String> = Users

    val databaseReads: Int
        get() = readCounter.get()

    override fun ResultRow.toEntity(): UserResponse =
        UserResponse(
            id = this[Users.id].value,
            displayName = this[Users.displayName],
            version = this[Users.version],
            source = "database",
        )

    override fun UpdateStatement.updateEntity(entity: UserResponse) {
        this[Users.displayName] = entity.displayName
        this[Users.version] = entity.version
    }

    override fun BatchInsertStatement.insertEntity(entity: UserResponse) {
        this[Users.id] = EntityID(entity.id, Users)
        this[Users.displayName] = entity.displayName
        this[Users.version] = entity.version
    }

    override fun extractId(entity: UserResponse): String = entity.id

    override fun findByIdFromDb(id: String): UserResponse? {
        assertOwner()
        readCounter.incrementAndGet()
        return super.findByIdFromDb(id)
    }

    override fun get(id: String): UserResponse? {
        assertOwner()
        return super.get(id)
    }

    override fun put(id: String, entity: UserResponse) {
        assertOwner()
        super.put(id, entity)
    }

    override fun invalidate(id: String) {
        assertOwner()
        super.invalidate(id)
    }

    fun nextVersion(id: String): Int {
        assertOwner()
        return transaction {
            Users
                .selectAll()
                .where { Users.id eq id }
                .singleOrNull()
                ?.get(Users.version)
                ?: 0
        } + 1
    }
}
