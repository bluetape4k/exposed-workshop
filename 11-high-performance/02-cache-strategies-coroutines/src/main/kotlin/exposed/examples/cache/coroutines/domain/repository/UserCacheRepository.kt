package exposed.examples.cache.coroutines.domain.repository

import exposed.examples.cache.coroutines.domain.model.UserRecord
import exposed.examples.cache.coroutines.domain.model.UserTable
import exposed.examples.cache.coroutines.domain.model.toUserRecord
import io.bluetape4k.exposed.redisson.repository.AbstractSuspendedJdbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Read Through / Write Through 를 이용해 DB의 사용자 정보를 캐시합니다.
 */
@Repository
class UserCacheRepository(redissonClient: RedissonClient):
    AbstractSuspendedJdbcRedissonRepository<Long, UserRecord>(
        redissonClient = redissonClient,
        config = RedissonCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
            deleteFromDBOnInvalidate = true,
            name = "exposed:coroutines:users"
        )
    ) {
    companion object: KLoggingChannel()


    override val table: IdTable<Long> = UserTable
    override fun ResultRow.toEntity() = toUserRecord()
    override fun extractId(entity: UserRecord): Long = entity.id

    override fun UpdateStatement.updateEntity(entity: UserRecord) {
        log.debug { "Update entity: $entity" }
        this[UserTable.username] = entity.username
        this[UserTable.firstName] = entity.firstName
        this[UserTable.lastName] = entity.lastName
        this[UserTable.address] = entity.address
        this[UserTable.zipcode] = entity.zipcode
        this[UserTable.birthDate] = entity.birthDate
        this[UserTable.avatar] = entity.avatar?.let { ExposedBlob(it) }
        this[UserTable.updatedAt] = Instant.now()
    }

    override fun BatchInsertStatement.insertEntity(entity: UserRecord) {
        log.debug { "Insert entity: $entity" }
        if (entity.id != 0L) {
            this[UserTable.id] = entity.id
        }
        this[UserTable.username] = entity.username
        this[UserTable.firstName] = entity.firstName
        this[UserTable.lastName] = entity.lastName
        this[UserTable.address] = entity.address
        this[UserTable.zipcode] = entity.zipcode
        this[UserTable.birthDate] = entity.birthDate
        this[UserTable.avatar] = entity.avatar?.let { ExposedBlob(it) }
        this[UserTable.createdAt] = Instant.now()
    }
}
