package exposed.examples.spring.modulith.publications.config

import exposed.examples.spring.modulith.publications.fulfillment.FulfillmentReservations
import exposed.examples.spring.modulith.publications.orders.WorkshopOrders
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.modulith.events.core.EventSerializer
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
class WorkshopDatabaseConfig {

    @Bean("springTransactionManager")
    fun springTransactionManager(dataSource: DataSource): SpringTransactionManager =
        SpringTransactionManager(dataSource, DatabaseConfig {}, false)

    @Bean
    fun eventSerializer(): EventSerializer {
        val mapper = JsonMapper.builder()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addModule(KotlinModule.Builder().build())
            .build()

        return object : EventSerializer {
            override fun serialize(event: Any): Any =
                mapper.writeValueAsString(event)

            override fun <T : Any> deserialize(serialized: Any, type: Class<T>): T =
                mapper.readerFor(type).readValue(serialized.toString())
        }
    }

    @Bean
    fun workshopSchemaInitializer(
        @Qualifier("transactionTemplate") transactionTemplate: TransactionTemplate,
    ): SmartInitializingSingleton =
        SmartInitializingSingleton {
            transactionTemplate.executeWithoutResult {
                SchemaUtils.create(WorkshopOrders, FulfillmentReservations)
            }
        }
}
