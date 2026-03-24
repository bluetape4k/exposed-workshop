package exposed.examples.cache.coroutines

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.junit.jupiter.api.BeforeAll
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/** 코루틴 기반 캐시 전략 예제의 Spring Boot 통합 테스트를 위한 추상 베이스 클래스입니다. */
@SpringBootTest(
    classes = [CacheStrategyApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractCacheStrategyTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val faker = Fakers.faker

        /**
         * Spring 컨텍스트가 공유되므로 Redis FLUSHALL은 전체 테스트 스위트에서 최초 1회만 실행합니다.
         * 컨테이너 재사용(reuse=true)으로 인해 이전 JVM 실행에서 남은 Write-Behind 잔여 데이터,
         * 스테일 캐시 항목 등을 정리하여 깨끗한 상태에서 테스트를 시작합니다.
         */
        @Volatile
        private var redisInitialized = false
    }

    @Autowired
    private lateinit var redissonClient: RedissonClient

    /**
     * 테스트 클래스 최초 실행 전, Redis의 모든 데이터를 삭제합니다.
     *
     * - 컨테이너 재사용(reuse=true)으로 인한 이전 JVM의 잔존 데이터를 정리합니다.
     * - Write-Behind 미완료 큐, 스테일 Near Cache 항목 등을 포함한 모든 Redis 키를 제거합니다.
     */
    @BeforeAll
    fun flushRedisBeforeTests() {
        if (!redisInitialized) {
            log.info { "Redis 캐시 키 초기화: exposed:* 패턴의 잔존 데이터를 제거합니다." }
            redissonClient.keys.deleteByPattern("*exposed:*")
            redisInitialized = true
            log.info { "Redis 캐시 키 초기화 완료." }
        }
    }
}
