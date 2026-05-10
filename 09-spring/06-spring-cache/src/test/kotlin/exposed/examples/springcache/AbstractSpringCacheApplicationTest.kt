package exposed.examples.springcache

import io.bluetape4k.logging.KLogging
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(classes = [SpringCacheApplication::class])
abstract class AbstractSpringCacheApplicationTest {

    companion object: KLogging() {
        private val redisServer get() = SpringCacheApplication.redisServer

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisServer.host }
            registry.add("spring.data.redis.port") { redisServer.port }
        }
    }
}
