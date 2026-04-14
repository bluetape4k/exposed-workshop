package exposed.examples.cache.domain.repository

import exposed.examples.cache.domain.model.UserCredentialsRecord
import exposed.examples.cache.domain.model.UserCredentialsTable
import exposed.examples.cache.domain.model.toUserCredentialsRecord
import io.bluetape4k.exposed.redisson.repository.AbstractJdbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Repository

/**
 * 사용자 인증 정보를 Read-Only 전략으로 조회하는 캐시 저장소입니다.
 *
 * - 조회 시: 캐시에 없으면 DB를 조회해 캐시에 적재합니다.
 * - 저장/수정 시: 이 저장소에서는 처리하지 않습니다.
 */
@Repository
class UserCredentialsCacheRepository(
    redissonClient: RedissonClient,
): AbstractJdbcRedissonRepository<String, UserCredentialsRecord>(
    redissonClient = redissonClient,
    config = RedissonCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(name = "exposed:user-credentials"),
) {

    companion object: KLoggingChannel()

    override val table = UserCredentialsTable
    override fun ResultRow.toEntity() = toUserCredentialsRecord()
    override fun extractId(entity: UserCredentialsRecord): String = entity.id

    // READ-ONLY 이므로,  updateEntity, insertEntity 에서 아무 작업도 하지 않습니다.

    override fun BatchInsertStatement.insertEntity(entity: UserCredentialsRecord) {
        // No operation
    }

    override fun UpdateStatement.updateEntity(entity: UserCredentialsRecord) {
        // No operation
    }
}
