package exposed.examples.transaction.config

import exposed.examples.transaction.AbstractTransactionApplicationTest
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
class TransactionTemplateConfigTest: AbstractTransactionApplicationTest() {

    companion object: KLogging()

    @Autowired
    @Qualifier("exposedTransactionTemplate")
    private val exposedTransactionTemplate: TransactionOperations = uninitialized()

    @Autowired
    @Qualifier("withoutTransactionOperations")
    private val withoutTransactionOperations: TransactionOperations = uninitialized()


    @Test
    fun `context loading`() {
        exposedTransactionTemplate.shouldNotBeNull()
        withoutTransactionOperations.shouldNotBeNull()
    }
}
