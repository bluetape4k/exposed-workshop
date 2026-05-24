package exposed.multitenant.onboarding.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.onboarding.service.TenantOnboardingService
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
class TenantOnboardingConfig {

    @Bean
    fun dataSource(): HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:tenant_onboarding;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                maximumPoolSize = 5
                isAutoCommit = false
            },
        )

    @Bean
    fun database(dataSource: DataSource): Database =
        Database.connect(dataSource)

    @Bean
    fun tenantOnboardingService(database: Database): TenantOnboardingService =
        TenantOnboardingService(database).also {
            it.initializeCatalog()
        }
}
