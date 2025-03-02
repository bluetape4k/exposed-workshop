package exposed.examples.jdbc.config

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.spring.SpringTransactionManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionOperations
import org.springframework.transaction.support.TransactionTemplate

@Configuration
class JdbcConfiguration {

    companion object: KLogging()

    /**
     * Exposed의 [SpringTransactionManager]를 사용하여 Spring Transaction을 수행하는 [TransactionOperations] Bean
     */
    @Bean
    @Qualifier("operations1")
    fun operations1(transactionManager: SpringTransactionManager): TransactionTemplate {
        return TransactionTemplate(transactionManager)
    }

    /**
     * Trasaction 없이 수행하는 [TransactionOperations] Bean
     */
    @Bean
    @Qualifier("operations2")
    fun operation2(): TransactionOperations =
        TransactionOperations.withoutTransaction()
}
