package exposed.examples.springwebflux

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2")   // h2 | mysql | postgres 중에 하나를 선택하세요
@AutoConfigureWebTestClient
@SpringBootTest(
    classes = [CoroutineExposedRepositoryApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractCoroutineExposedRepositoryTest {

    @Autowired
    protected lateinit var database: Database

    companion object: KLoggingChannel() {
        @JvmStatic
        val faker = Fakers.faker
    }
}
