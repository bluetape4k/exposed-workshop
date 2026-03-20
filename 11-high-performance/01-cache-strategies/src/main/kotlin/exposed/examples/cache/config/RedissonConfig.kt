package exposed.examples.cache.config

import exposed.examples.cache.CacheStrategyApplication.Companion.redis
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Redisson 클라이언트와 연결 설정을 제공합니다.
 */
@Configuration(proxyBeanMethods = false)
class RedissonConfig {
    companion object: KLogging()

    @Bean
    fun redissonClient(): RedissonClient {
        return RedisServer.Launcher.RedissonLib.getRedisson(redis.url)
    }
}
