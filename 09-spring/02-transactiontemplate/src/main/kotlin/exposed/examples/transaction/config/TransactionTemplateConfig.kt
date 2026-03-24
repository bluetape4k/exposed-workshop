package exposed.examples.transaction.config

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionOperations
import org.springframework.transaction.support.TransactionTemplate

/**
 * Exposed 기반 트랜잭션 템플릿과 트랜잭션 미적용 템플릿을 구성하는 설정입니다.
 */
@Configuration(proxyBeanMethods = false)
class TransactionTemplateConfig {

    companion object: KLogging()

    /**
     * Exposed의 [SpringTransactionManager]를 사용하여 Spring Transaction을 수행하는 [TransactionTemplate] Bean
     */
    @Bean
    @Qualifier("exposedTransactionTemplate")
    fun exposedTransactionTemplate(tm: SpringTransactionManager): TransactionTemplate =
        TransactionTemplate(tm)

    /**
     * 트랜잭션 없이 실행하는 [TransactionOperations] 빈입니다.
     */
    @Bean
    @Qualifier("withoutTransactionOperations")
    fun withoutTransactionOperations(): TransactionOperations =
        TransactionOperations.withoutTransaction()
}
