package exposed.examples.suspendedcache.config

import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCacheManager
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.lettuce.core.RedisClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LettuceSuspendedCacheConfig(
    private val redisClient: RedisClient,
) {

    companion object: KLoggingChannel()

    @Bean
    fun lettuceSuspendedCacheManager(): LettuceSuspendedCacheManager {
        return LettuceSuspendedCacheManager(
            redisClient = redisClient,
            ttlSeconds = 60L,
            codec = LettuceBinaryCodecs.lz4Fory(),
        )
    }
}
