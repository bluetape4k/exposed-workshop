package exposed.examples.jdbc.config

import exposed.examples.jdbc.AbstractTransactionApplicationTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.transaction.support.TransactionOperations

/**
 * Spring Transaction을 수행하는 [TransactionOperations] Bean 을 사용하기 위한 설정이 제대로 되었는지 확인합니다.
 */
class TransactionConfigurationTest: AbstractTransactionApplicationTest() {

    companion object: KLogging()

    @Autowired
    @Qualifier("operations1")
    private val operations1: TransactionOperations = uninitialized()

    @Autowired
    @Qualifier("operations2")
    private val operations2: TransactionOperations = uninitialized()

    @Test
    fun `context loading`() {
        operations1.shouldNotBeNull()
        operations2.shouldNotBeNull()
    }
}
