package exposed.examples.springmvc

import io.bluetape4k.junit5.faker.Fakers
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("postgres")   // h2 | mysql | postgres 중에 하나를 선택하세요
@SpringBootTest(
    classes = [ExposedRepositoryApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractExposedRepositoryTest {

    companion object {
        @JvmStatic
        val faker = Fakers.faker
    }
}
