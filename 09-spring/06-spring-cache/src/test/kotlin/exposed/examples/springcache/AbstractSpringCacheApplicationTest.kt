package exposed.examples.springcache

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SpringCacheApplication::class])
abstract class AbstractSpringCacheApplicationTest {

    companion object: KLogging()

    @Autowired
    private lateinit var database: Database

    @BeforeEach
    fun resetDefaultDatabase() {
        TransactionManager.defaultDatabase = database
    }
}
