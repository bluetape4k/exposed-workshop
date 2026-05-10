package exposed.workshop.springmvc

import io.bluetape4k.junit5.faker.Fakers
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2") // h2 | postgres | mysql
@AutoConfigureWebTestClient
@SpringBootTest(
    classes = [SpringMvcApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractSpringMvcTest {

    @Autowired
    protected lateinit var database: Database

    companion object {
        @JvmStatic
        val faker = Fakers.faker
    }
}
