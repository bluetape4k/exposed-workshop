package exposed.examples.spring.transaction

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager

@SpringBootTest(classes = [SpringTransactionApp::class])
abstract class AbstractSpringTransactionTest {

    companion object: KLogging()

    @Autowired
    protected val ctx: ApplicationContext = uninitialized()

    @Autowired
    protected val transactionManager: PlatformTransactionManager = uninitialized()

}
