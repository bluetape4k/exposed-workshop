package exposed.examples.spring.transaction.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.examples.spring.transaction.service.OrderService
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.DatabaseConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.TransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.TransactionManagementConfigurer
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class DataSourceConfig: TransactionManagementConfigurer {

    companion object: KLogging()

    @Bean
    fun dataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:${Base58.randomString(8)};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL"
            driverClassName = "org.h2.Driver"
            username = "root"
            password = ""
        }

        return HikariDataSource(config)
    }

    @Bean
    override fun annotationDrivenTransactionManager(): TransactionManager {
        log.info { "Create Exposed's SpringTransactionManager" }
        return SpringTransactionManager(dataSource(), DatabaseConfig { useNestedTransactions = true })
    }

    @Bean
    fun orderService(): OrderService = OrderService()
}
