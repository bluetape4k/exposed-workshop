package exposed.examples.cache.domain.repository

import exposed.examples.cache.domain.model.UserRecord
import exposed.examples.cache.domain.model.UserTable
import exposed.examples.cache.domain.model.toUserRecord
import io.bluetape4k.exposed.redisson.repository.AbstractJdbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
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
class UserCacheRepository(redissonClient: RedissonClient): AbstractJdbcRedissonRepository<Long, UserRecord>(
    redissonClient = redissonClient,
    config = RedissonCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
        name = "exposed:users",
        deleteFromDBOnInvalidate = true
    )
) {
    companion object: KLoggingChannel()

    override val table: UserTable = UserTable
    override fun ResultRow.toEntity(): UserRecord = toUserRecord()
    override fun extractId(entity: UserRecord): Long = entity.id

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


}
