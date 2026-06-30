package exposed.examples.spring.modulith.boundaries.orders

import exposed.examples.spring.modulith.boundaries.orders.internal.WorkshopOrders
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionTemplate

@Configuration(proxyBeanMethods = false)
class OrderSchemaConfig {

    @Bean
    fun orderSchemaInitializer(transactionTemplate: TransactionTemplate): SmartInitializingSingleton =
        SmartInitializingSingleton {
            transactionTemplate.executeWithoutResult {
                SchemaUtils.create(WorkshopOrders)
            }
        }
}
