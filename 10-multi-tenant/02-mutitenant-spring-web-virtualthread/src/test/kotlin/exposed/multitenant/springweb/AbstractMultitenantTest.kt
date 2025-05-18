package exposed.multitenant.springweb

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2")
@SpringBootTest(
    classes = [ExposedMultitenantApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractMultitenantTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val faker = Fakers.faker
    }
}
