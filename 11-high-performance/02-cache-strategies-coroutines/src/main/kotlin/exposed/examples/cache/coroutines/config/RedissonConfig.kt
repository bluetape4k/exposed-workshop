package exposed.examples.cache.coroutines.config

import exposed.examples.cache.coroutines.CacheStrategyApplication.Companion.redis
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.RedisServer
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 코루틴 캐시 예제에서 Redis 연결에 사용할 Redisson 클라이언트를 제공합니다.
 */
@Configuration(proxyBeanMethods = false)
class RedissonConfig {
    companion object: KLoggingChannel()

    /**
     * 단일 Redis 노드에 연결하는 [RedissonClient]를 생성합니다.
     */
    @Bean
    fun redissonClient(): RedissonClient {
        return RedisServer.Launcher.RedissonLib.getRedisson(redis.url)
    }
}
