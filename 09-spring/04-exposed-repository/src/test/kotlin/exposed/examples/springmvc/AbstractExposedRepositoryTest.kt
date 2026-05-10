package exposed.examples.springmvc

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2")   // h2 | mysql | postgres 중에 하나를 선택하세요
@AutoConfigureWebTestClient
@SpringBootTest(
    classes = [ExposedRepositoryApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractExposedRepositoryTest {

    @Autowired
    protected lateinit var database: Database

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }
}
