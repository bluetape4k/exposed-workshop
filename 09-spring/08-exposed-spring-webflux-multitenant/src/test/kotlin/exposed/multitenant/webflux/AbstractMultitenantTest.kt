package exposed.multitenant.webflux

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2") // h2 | postgres 중에 하나를 선택할 수 있습니다.
@SpringBootTest(
    classes = [ExposedMultitenantWebfluxApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractMultitenantTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }

}
