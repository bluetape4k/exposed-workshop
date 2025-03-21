package exposed.multitenant.springweb

import io.bluetape4k.logging.KLogging
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("postgres")
@SpringBootTest(
    classes = [ExposedMultitenantApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractMultitenantTest {
    companion object: KLogging()
}
