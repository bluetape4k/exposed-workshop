package exposed.examples.cache.config

import exposed.examples.cache.CacheStrategyApplication.Companion.redis
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
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
                .setConnectionPoolSize(128)
                .setConnectionMinimumIdleSize(32) // 최소 연결을 충분히 확보하여 Latency 방지
                .setIdleConnectionTimeout(10000)  // 연결 유지를 넉넉히 (10초)
                .setTimeout(2000)
                .setRetryAttempts(3)
                .setRetryDelay { attempt -> Duration.ofMillis((attempt + 1) * 100L) }

                .setDnsMonitoringInterval(5000)  // DNS 변경 감지 (Cloud 환경 필수)

            nettyThreads = 128
            codec = RedissonCodecs.LZ4ForyComposite
            setTcpNoDelay(true)
        }

        return Redisson.create(config)
    }
}
