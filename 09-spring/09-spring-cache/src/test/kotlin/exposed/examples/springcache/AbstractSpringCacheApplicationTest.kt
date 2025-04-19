package exposed.examples.springcache

import io.bluetape4k.logging.KLogging
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SpringCacheApplication::class])
abstract class AbstractSpringCacheApplicationTest {

    companion object: KLogging()
}
