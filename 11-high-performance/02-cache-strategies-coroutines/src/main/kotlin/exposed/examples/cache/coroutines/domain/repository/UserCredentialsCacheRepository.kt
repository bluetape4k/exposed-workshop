package exposed.examples.cache.coroutines.domain.repository

import exposed.examples.cache.coroutines.domain.model.UserCredentialsDTO
import exposed.examples.cache.coroutines.domain.model.UserCredentialsTable
import exposed.examples.cache.coroutines.domain.model.toUserCredentialsDTO
import io.bluetape4k.exposed.redisson.repository.AbstractSuspendedExposedCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.sql.ResultRow
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Repository

/**
 * Read-Only 캐시를 이용하여, DB의 사용자 인증 정보를 캐시합니다.
 */
@Repository
class UserCredentialsCacheRepository(
    redissonClient: RedissonClient,
): AbstractSuspendedExposedCacheRepository<UserCredentialsDTO, String>(
    redissonClient = redissonClient,
    cacheName = "exposed:coroutines:user-credentials",
    config = RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE,
) {

    companion object: KLoggingChannel()

    override val entityTable = UserCredentialsTable
    override fun ResultRow.toEntity() = toUserCredentialsDTO()

    // READ-ONLY 이므로, doUpdateEntity, doInsertEntity 를 구현하지 않습니다.
}
