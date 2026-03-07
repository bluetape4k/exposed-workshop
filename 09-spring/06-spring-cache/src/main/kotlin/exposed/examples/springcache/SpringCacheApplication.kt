package exposed.examples.springcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Cache와 Redis를 활용한 Exposed 캐시 예제 애플리케이션.
 *
 * Spring의 캐시 추상화(@Cacheable, @CacheEvict 등)를 Exposed와 통합하는 예제입니다.
 * 테스트 환경에서 Redis 서버를 자동으로 시작합니다.
 */
@SpringBootApplication
class SpringCacheApplication {

    companion object: KLogging() {
        /** 테스트용 Redis 서버 인스턴스 */
        @JvmStatic
        val redisServer = RedisServer.Launcher.redis
    }
}

/**
 * 애플리케이션 진입점.
 *
 * @param args 커맨드라인 인자
 */
fun main(vararg args: String) {
    runApplication<SpringCacheApplication>(*args)
}
