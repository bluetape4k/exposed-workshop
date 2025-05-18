package alternative.hibernate.reactive.example

import alternatives.hibernate.reactive.example.HibernateReactiveApplication
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("postgres")
@SpringBootTest(
    classes = [HibernateReactiveApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractHibernateReactiveTest {

    companion object: KLoggingChannel() {

        @JvmStatic
        val faker = Fakers.faker
    }
}
