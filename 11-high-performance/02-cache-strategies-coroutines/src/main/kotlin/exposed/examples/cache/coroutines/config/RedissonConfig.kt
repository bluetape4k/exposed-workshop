package exposed.examples.cache.coroutines.config

import exposed.examples.cache.coroutines.CacheStrategyApplication.Companion.redis
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

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
        val config =
            Config().apply {
                useSingleServer()
                    .setAddress(redis.url)
                    .setConnectionPoolSize(256)
                    .setConnectionMinimumIdleSize(32) // 최소 연결을 충분히 확보하여 Latency 방지
                    .setIdleConnectionTimeout(60000) // idle 연결 유지 시간 (60초) - Spring 컨텍스트 로딩 시간 고려
                    .setTimeout(5000)
                    .setRetryAttempts(5)
                    .setRetryDelay { attempt -> Duration.ofMillis((attempt + 1) * 200L) }
                    .setDnsMonitoringInterval(-1) // 테스트 환경에서는 DNS 모니터링 비활성화 (불필요한 연결 재생성 방지)

                executor = VirtualThreadExecutor
                threads = 256
                nettyThreads = 128
                codec = RedissonCodecs.LZ4ForyComposite
                setTcpNoDelay(true)
                // setTcpUserTimeout: Linux 전용 소켓 옵션 - Docker Desktop(macOS)에서 간헐적 채널 종료 유발하므로 제거
            }

        return Redisson.create(config).also { client ->
            warmupPubSubChannel(client)
        }
    }

    /**
     * Testcontainers 첫 실행 시 Docker 포트 프록시 준비 전에 pub/sub 채널 연결을 시도하면
     * StacklessClosedChannelException 이 발생합니다.
     * 더미 패턴 토픽에 구독/해제를 반복하여 pub/sub 채널을 미리 워밍업합니다.
     */
    private fun warmupPubSubChannel(client: RedissonClient) {
        repeat(5) { attempt ->
            runCatching {
                val topic = client.getPatternTopic("__warmup__*")
                val listenerId = topic.addListener(Any::class.java) { _, _, _ -> }
                topic.removeListener(listenerId)
                return
            }.onFailure { e ->
                log.warn { "pub/sub 워밍업 실패 (attempt ${attempt + 1}/5): ${e.message}" }
                Thread.sleep((attempt + 1) * 300L)
            }
        }
    }
}
