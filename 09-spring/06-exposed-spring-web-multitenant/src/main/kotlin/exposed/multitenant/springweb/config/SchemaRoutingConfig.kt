package exposed.multitenant.springweb.config

import exposed.multitenant.springweb.tenant.TenantAwareDataSource
import exposed.multitenant.springweb.tenant.Tenants
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class SchemaRoutingConfig {

    companion object: KLogging() {

        private val postgres by lazy {
            PostgreSQLServer.Launcher.postgres
        }
    }

    @Bean
    fun database(dataSource: DataSource, databaseConfig: DatabaseConfig): Database {
        log.info { "Database connection: $dataSource" }

        return Database.connect(dataSource, databaseConfig = databaseConfig)
    }

    /**
     * Active Profile 에 해당하는 DataSource (H2, PostgreSQL) 를 입력받아, Tenant 별로 DataSource 를 설정합니다.
     */
    @Suppress("UNCHECKED_CAST")
    @Primary
    @Bean
    fun tenantAwareDataSource(dataSource: DataSource): TenantAwareDataSource {
        val tenantDataSource = TenantAwareDataSource()

        val targetDataSources = Tenants.Tenant.entries.associateWith { dataSource }

        tenantDataSource.setTargetDataSources(targetDataSources as Map<Any, Any>)
        tenantDataSource.setDefaultTargetDataSource(targetDataSources[Tenants.DEFAULT_TENANT] as Any)
        tenantDataSource.afterPropertiesSet()

        return tenantDataSource
    }
}
