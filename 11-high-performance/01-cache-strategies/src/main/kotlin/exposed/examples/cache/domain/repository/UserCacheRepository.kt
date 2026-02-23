package exposed.examples.cache.domain.repository

import exposed.examples.cache.domain.model.UserRecord
import exposed.examples.cache.domain.model.UserTable
import exposed.examples.cache.domain.model.toUserRecord
import io.bluetape4k.exposed.redisson.repository.AbstractExposedCacheRepository
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
 * 사용자 정보를 Read Through/Write Through 전략으로 조회 및 저장하는 캐시 저장소입니다.
 *
 * - 조회 시: 캐시에 없으면 DB에서 읽어 캐시에 채웁니다.
 * - 저장 시: 캐시와 DB를 함께 갱신합니다.
 */
@Repository
class UserCacheRepository(redissonClient: RedissonClient): AbstractExposedCacheRepository<UserRecord, Long>(
    redissonClient = redissonClient,
    cacheName = "exposed:users",
    config = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(deleteFromDBOnInvalidate = true)
) {
    companion object: KLoggingChannel()

    override val entityTable = UserTable
    override fun ResultRow.toEntity() = toUserRecord()

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserRecord,
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

    override fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: UserRecord,
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
}
