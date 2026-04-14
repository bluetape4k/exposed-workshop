package exposed.examples.cache.coroutines.domain.repository

import exposed.examples.cache.coroutines.domain.model.UserEventRecord
import exposed.examples.cache.coroutines.domain.model.UserEventTable
import exposed.examples.cache.coroutines.domain.model.toUserEventRecord
import io.bluetape4k.exposed.redisson.repository.AbstractSuspendedJdbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Repository

/**
 * Write-Behind 캐시를 이용하여, 대량의 데이터를 비동기적으로 DB에 저장합니다.
 */
@Repository
class UserEventCacheRepository(
    redissonClient: RedissonClient,
): AbstractSuspendedJdbcRedissonRepository<Long, UserEventRecord>(
    redissonClient = redissonClient,
    config = RedissonCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE.copy(name = "exposed:coroutines:user-events"),
) {

    companion object: KLoggingChannel()

    override val table: IdTable<Long> = UserEventTable
    override fun ResultRow.toEntity(): UserEventRecord = toUserEventRecord()
    override fun extractId(entity: UserEventRecord): Long = entity.id

    override fun BatchInsertStatement.insertEntity(entity: UserEventRecord) {
        log.debug { "Insert entity: $entity" }

        if (entity.id != 0L) {
            this[UserEventTable.id] = entity.id
        }

        this[UserEventTable.username] = entity.username
        this[UserEventTable.eventSource] = entity.eventSource
        this[UserEventTable.eventType] = entity.eventType
        this[UserEventTable.eventDetails] = entity.eventDetails
        this[UserEventTable.eventTime] = entity.eventTime
    }

    override fun UpdateStatement.updateEntity(entity: UserEventRecord) {
        log.debug { "Update entity: $entity" }

        this[UserEventTable.username] = entity.username
        this[UserEventTable.eventSource] = entity.eventSource
        this[UserEventTable.eventType] = entity.eventType
        this[UserEventTable.eventDetails] = entity.eventDetails
        this[UserEventTable.eventTime] = entity.eventTime
    }


}
