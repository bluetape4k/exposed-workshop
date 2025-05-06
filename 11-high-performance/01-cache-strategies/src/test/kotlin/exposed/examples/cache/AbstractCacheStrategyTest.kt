package exposed.examples.cache

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [CacheStrategyApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractCacheStrategyTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }
}
