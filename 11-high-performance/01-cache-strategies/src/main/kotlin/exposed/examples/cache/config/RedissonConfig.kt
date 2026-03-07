package exposed.examples.cache.config

import exposed.examples.cache.CacheStrategyApplication.Companion.redis
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.logging.KLogging
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Redisson 클라이언트와 연결 설정을 제공합니다.
 */
@Configuration(proxyBeanMethods = false)
class RedissonConfig {

    companion object: KLogging()

    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config().apply {
            useSingleServer()
                .setAddress(redis.url)
                .setConnectionPoolSize(100)
                .setConnectionMinimumIdleSize(10)
                .setIdleConnectionTimeout(1000)
                .setTimeout(1000)
                .setRetryAttempts(3)
                .setRetryDelay { attempt -> Duration.ofMillis((attempt + 1) * 100L) }

            executor = VirtualThreadExecutor
            nettyExecutor = VirtualThreadExecutor
            nettyThreads = 64
        }

        return Redisson.create(config)
    }
}
