package exposed.workshop.springwebflux

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2")  // h2 | mysql | postgres 를 사용할 수 있습니다.
@AutoConfigureWebTestClient
@SpringBootTest(
    classes = [SpringWebfluxApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractSpringWebfluxTest {

    @Autowired
    protected lateinit var database: Database

    companion object: KLoggingChannel() {
        @JvmStatic
        val faker = Fakers.faker
    }
}
