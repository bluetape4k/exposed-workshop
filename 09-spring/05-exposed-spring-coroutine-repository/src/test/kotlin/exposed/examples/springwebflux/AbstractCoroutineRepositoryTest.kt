package exposed.examples.springwebflux

import io.bluetape4k.logging.KLogging
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [CoroutineRepositoryApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractCoroutineRepositoryTest {

    companion object: KLogging()

}
