package exposed.examples.spring.transaction.utils

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate

/**
 * [PlatformTransactionManager] 의 Transaction 하에서 [block] 을 실행합니다.
 */
fun PlatformTransactionManager.execute(
    propagationBehavior: Int = TransactionDefinition.PROPAGATION_REQUIRED,
    isolationLevel: Int = TransactionDefinition.ISOLATION_DEFAULT,
    readOnly: Boolean = false,
    timeout: Int? = null,
    block: (TransactionStatus) -> Unit,
) {
    if (this !is org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager) {
        error("Wrong transaction manager. ${this.javaClass.name}, use Exposed's SpringTransactionManager")
    }

    val txTemplate = TransactionTemplate(this).also {
        it.propagationBehavior = propagationBehavior
        it.isolationLevel = isolationLevel
        if (readOnly) it.isReadOnly = true
        timeout?.run { it.timeout = timeout }
    }

    txTemplate.executeWithoutResult {
        block(it)
    }
}
