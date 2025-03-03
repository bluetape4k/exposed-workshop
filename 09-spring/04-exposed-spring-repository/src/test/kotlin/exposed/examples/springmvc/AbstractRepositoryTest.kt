package exposed.examples.springmvc

import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RepositoryApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class AbstractRepositoryTest {
}
