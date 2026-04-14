package exposed.examples.cache.coroutines.domain.repository

import exposed.examples.cache.coroutines.domain.model.UserCredentialsRecord
import exposed.examples.cache.coroutines.domain.model.UserCredentialsTable
import exposed.examples.cache.coroutines.domain.model.toUserCredentialsRecord
import io.bluetape4k.exposed.redisson.repository.AbstractSuspendedJdbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Repository

/**
 * Read-Only 캐시를 이용하여, DB의 사용자 인증 정보를 캐시합니다.
 */
@Repository
class UserCredentialsCacheRepository(
    redissonClient: RedissonClient,
): AbstractSuspendedJdbcRedissonRepository<String, UserCredentialsRecord>(
    redissonClient = redissonClient,
    config = RedissonCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(name = "exposed:coroutines:user-credentials"),
) {

    companion object: KLoggingChannel()

    override val table: IdTable<String> = UserCredentialsTable
    override fun ResultRow.toEntity() = toUserCredentialsRecord()
    override fun extractId(entity: UserCredentialsRecord): String = entity.id


    override fun UpdateStatement.updateEntity(entity: UserCredentialsRecord) {
        // READ-ONLY 이므로, insertEntity, updateEntity 는 아무 작업을 하지 않습니다.
    }

    override fun BatchInsertStatement.insertEntity(entity: UserCredentialsRecord) {
        // READ-ONLY 이므로, insertEntity, updateEntity 는 아무 작업을 하지 않습니다.
    }


}
