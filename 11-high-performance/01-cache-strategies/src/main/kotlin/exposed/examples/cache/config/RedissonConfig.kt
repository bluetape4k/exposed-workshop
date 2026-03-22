package exposed.examples.cache.config

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.warn
import io.bluetape4k.testcontainers.storage.RedisServer
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Redisson 클라이언트와 연결 설정을 제공합니다.
 */
@Configuration(proxyBeanMethods = false)
class RedissonConfig {
    companion object: KLoggingChannel() {
        @JvmStatic
        val redis by lazy { RedisServer.Launcher.redis }
    }

    /**
     * 단일 Redis 노드에 연결하는 [RedissonClient]를 생성합니다.
     */
    @Bean
    fun redissonClient(): RedissonClient {
        val server = redis
        var client: RedissonClient? = null

        // Redis 가 준비 안되었을 경우를 대비해서 ...
        while (client == null) {
            try {
                client = RedisServer.Launcher.RedissonLib.getRedisson(
                    address = server.url,
                    threads = 64
                )
            } catch (e: Exception) {
                log.warn(e) { "Fail to connect reds" }
            }
        }
        return client
    }
}
