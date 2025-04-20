package exposed.examples.suspendedcache

import io.bluetape4k.logging.KLogging
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SpringSuspendedCacheApplication::class])
abstract class AbstractSpringSuspendedCacheApplicationTest {

    companion object: KLogging()

}
