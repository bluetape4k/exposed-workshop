package exposed.workshop.springmvc

import io.bluetape4k.junit5.faker.Fakers
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2")
@SpringBootTest(
    classes = [SpringMvcApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractSpringMvcTest {

    companion object {
        @JvmStatic
        val faker = Fakers.faker
    }
}
