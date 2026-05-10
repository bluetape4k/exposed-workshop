package exposed.examples.springmvc

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2")   // h2 | mysql | postgres 중에 하나를 선택하세요
@SpringBootTest(
    classes = [ExposedRepositoryApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
abstract class AbstractExposedRepositoryTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }

    @Autowired
    private lateinit var database: Database

    @BeforeEach
    fun resetDefaultDatabase() {
        TransactionManager.defaultDatabase = database
    }
}
