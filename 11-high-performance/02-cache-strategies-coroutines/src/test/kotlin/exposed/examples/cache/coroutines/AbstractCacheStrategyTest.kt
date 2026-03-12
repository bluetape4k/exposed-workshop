package exposed.examples.cache.coroutines

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
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
    }
}
