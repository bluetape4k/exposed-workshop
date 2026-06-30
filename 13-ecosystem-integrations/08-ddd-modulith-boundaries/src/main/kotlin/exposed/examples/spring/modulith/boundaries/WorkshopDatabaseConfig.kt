package exposed.examples.spring.modulith.boundaries

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
class WorkshopDatabaseConfig {

    @Bean("springTransactionManager")
    fun springTransactionManager(dataSource: DataSource): SpringTransactionManager =
        SpringTransactionManager(dataSource, DatabaseConfig {}, false)
}
