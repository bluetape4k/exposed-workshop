package exposed.workshop.springwebflux

import io.bluetape4k.logging.KLogging
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2")  // mysql, postgres 를 사용할 수 있습니다.
@SpringBootTest(
    classes = [SpringWebfluxApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractSpringWebfluxTest {

    companion object: KLogging()
}
