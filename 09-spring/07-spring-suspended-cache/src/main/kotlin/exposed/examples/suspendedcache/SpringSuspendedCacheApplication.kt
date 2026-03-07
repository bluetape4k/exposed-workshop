package exposed.examples.suspendedcache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

/**
 * 코루틴 기반의 Lettuce Redis 캐시를 활용한 Exposed 캐시 예제 애플리케이션.
 *
 * Spring WebFlux 환경에서 코루틴 suspend 함수와 Lettuce Redis 클라이언트를 통합하여
 * 비동기 캐시를 구현하는 예제입니다. 테스트 환경에서 Redis 서버를 자동으로 시작합니다.
 */
@SpringBootApplication
class SpringSuspendedCacheApplication {

    companion object: KLoggingChannel() {
        /** 테스트용 Redis 서버 인스턴스 */
        @JvmStatic
        val redisServer = RedisServer.Launcher.redis
    }

    /**
     * Lettuce Redis 클라이언트 Bean을 생성합니다.
     *
     * @return 테스트용 Redis 서버에 연결된 [RedisClient] 인스턴스
     */
    @Bean
    fun redisClient(): RedisClient {
        return RedisClient.create(RedisURI.create(redisServer.url))
    }
}

/**
 * 애플리케이션 진입점.
 *
 * @param args 커맨드라인 인자
 */
fun main(vararg args: String) {
    runApplication<SpringSuspendedCacheApplication>(*args)
}
