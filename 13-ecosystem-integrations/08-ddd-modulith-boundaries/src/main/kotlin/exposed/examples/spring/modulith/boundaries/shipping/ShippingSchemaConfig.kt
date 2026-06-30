package exposed.examples.spring.modulith.boundaries.shipping

import exposed.examples.spring.modulith.boundaries.shipping.internal.ShippingReservations
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionTemplate

@Configuration(proxyBeanMethods = false)
class ShippingSchemaConfig {

    @Bean
    fun shippingSchemaInitializer(transactionTemplate: TransactionTemplate): SmartInitializingSingleton =
        SmartInitializingSingleton {
            transactionTemplate.executeWithoutResult {
                SchemaUtils.create(ShippingReservations)
            }
        }
}
