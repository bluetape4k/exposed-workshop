package exposed.examples.cache.coroutines.domain.repository

import exposed.examples.cache.coroutines.domain.model.UserDTO
import exposed.examples.cache.coroutines.domain.model.UserTable
import exposed.examples.cache.coroutines.domain.model.toUserDTO
import io.bluetape4k.exposed.redisson.repository.AbstractSuspendedExposedCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
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
class UserCacheRepository(redissonClient: RedissonClient): AbstractSuspendedExposedCacheRepository<UserDTO, Long>(
    redissonClient = redissonClient,
    cacheName = "exposed:coroutines:users",
    config = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(deleteFromDBOnInvalidate = true)
) {
    companion object: KLoggingChannel()

    override val entityTable = UserTable
    override fun ResultRow.toEntity() = toUserDTO()

    override fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: UserDTO,
    ) {
        log.debug { "Insert entity: $entity" }
        if (entity.id != 0L) {
            statement[UserTable.id] = entity.id
        }
        statement[UserTable.username] = entity.username
        statement[UserTable.firstName] = entity.firstName
        statement[UserTable.lastName] = entity.lastName
        statement[UserTable.address] = entity.address
        statement[UserTable.zipcode] = entity.zipcode
        statement[UserTable.birthDate] = entity.birthDate
        statement[UserTable.avatar] = entity.avatar?.let { ExposedBlob(it) }
        statement[UserTable.createdAt] = Instant.now()
    }

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserDTO,
    ) {
        log.debug { "Update entity: $entity" }
        statement[UserTable.username] = entity.username
        statement[UserTable.firstName] = entity.firstName
        statement[UserTable.lastName] = entity.lastName
        statement[UserTable.address] = entity.address
        statement[UserTable.zipcode] = entity.zipcode
        statement[UserTable.birthDate] = entity.birthDate
        statement[UserTable.avatar] = entity.avatar?.let { ExposedBlob(it) }
        statement[UserTable.updatedAt] = Instant.now()
    }
}
