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
     *
     * `destroyMethod = ""`로 설정하여 Spring이 직접 `shutdown()`을 호출하지 않도록 합니다.
     * Redisson은 자체 JVM shutdown hook으로 생명주기를 관리하며, Spring이 중복 호출하면
     * [org.redisson.RedissonShutdownException]이 발생하는 문제를 방지합니다.
     */
    @Bean(destroyMethod = "")
    fun redissonClient(): RedissonClient {
        val server = redis
        var client: RedissonClient? = null
        var attempts = 0
        val maxAttempts = 10

        // Redis 가 준비 안되었을 경우를 대비해서 재시도 (최대 10회, 지수 백오프)
        while (client == null && attempts < maxAttempts) {
            try {
                client = RedisServer.Launcher.RedissonLib.getRedisson(
                    address = server.url,
                    threads = 64,
                    nettyThreads = 256
                )
                client.getAtomicLong("__test__")
            } catch (e: Exception) {
                client = null
                attempts++
                if (attempts >= maxAttempts) {
                    throw IllegalStateException("Redis 연결 실패 (${maxAttempts}회 시도 후)", e)
                }
                log.warn(e) { "Fail to connect redis (attempt $attempts/$maxAttempts)" }
                Thread.sleep(1000L * attempts)

            }
            // Redis Server 및 RedissonClient 가 준비될 수 있도록 대기한다
            Thread.sleep(100L * attempts)
        }
        return client ?: throw IllegalStateException("Redis 클라이언트 생성 실패")
    }
}
