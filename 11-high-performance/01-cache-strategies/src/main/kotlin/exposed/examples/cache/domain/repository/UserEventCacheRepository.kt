package exposed.examples.cache.domain.repository

import exposed.examples.cache.domain.model.UserEventDTO
import exposed.examples.cache.domain.model.UserEventTable
import exposed.examples.cache.domain.model.toUserEventDTO
import io.bluetape4k.exposed.redisson.repository.AbstractExposedCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Repository

/**
 * Write-Behind 캐시를 이용하여, 대량의 데이터를 비동기적으로 DB에 저장합니다.
 */
@Repository
class UserEventCacheRepository(
    redissonClient: RedissonClient,
): AbstractExposedCacheRepository<UserEventDTO, Long>(
    redissonClient = redissonClient,
    cacheName = "exposed:user-events",
    config = RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE,
) {

    companion object: KLoggingChannel()

    override val entityTable: IdTable<Long> = UserEventTable
    override fun ResultRow.toEntity(): UserEventDTO = toUserEventDTO()

    override fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: UserEventDTO,
    ) {
        log.debug { "Insert entity: $entity" }

        if (entity.id != 0L) {
            statement[UserEventTable.id] = entity.id
        }

        statement[UserEventTable.username] = entity.username
        statement[UserEventTable.eventSource] = entity.eventSource
        statement[UserEventTable.eventType] = entity.eventType
        statement[UserEventTable.eventDetails] = entity.eventDetails
        statement[UserEventTable.eventTime] = entity.eventTime
    }

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserEventDTO,
    ) {
        log.debug { "Update entity: $entity" }

        statement[UserEventTable.username] = entity.username
        statement[UserEventTable.eventSource] = entity.eventSource
        statement[UserEventTable.eventType] = entity.eventType
        statement[UserEventTable.eventDetails] = entity.eventDetails
        statement[UserEventTable.eventTime] = entity.eventTime
    }
}
