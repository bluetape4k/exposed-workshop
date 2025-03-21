package exposed.multitenant.springweb.config

import exposed.multitenant.springweb.tenant.TenantAwareDataSource
import exposed.multitenant.springweb.tenant.Tenants
import exposed.multitenant.springweb.tenant.Tenants.Tenant
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.database.JdbcServer
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.testcontainers.database.getDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
class SchemaRoutingConfig {

    companion object: KLogging()

    @Bean
    fun getJdbcServer(): JdbcServer {
        return PostgreSQLServer.Launcher.postgres
    }

    /**
     * Active Profile 에 해당하는 DataSource (H2, PostgreSQL) 를 입력받아, Tenant 별로 DataSource 를 설정합니다.
     */
    @Suppress("UNCHECKED_CAST")
    @Primary
    @Bean
    fun tenantAwareDataSource(jdbcServer: JdbcServer): TenantAwareDataSource {
        val tenantDataSource = TenantAwareDataSource()

        val targetDataSources = Tenant.entries.associateWith { jdbcServer.getDataSource() }

        tenantDataSource.setTargetDataSources(targetDataSources as Map<Any, Any>)
        tenantDataSource.setDefaultTargetDataSource(targetDataSources[Tenants.DEFAULT_TENANT] as Any)
        tenantDataSource.afterPropertiesSet()

        return tenantDataSource
    }

    @Bean
    fun databaseConfig(): DatabaseConfig {
        return DatabaseConfig {
            maxEntitiesToStoreInCachePerEntity = 100
            useNestedTransactions = true
        }
    }

    @Bean
    fun database(dataSource: TenantAwareDataSource, databaseConfig: DatabaseConfig): Database {
        log.info { "Database connection: $dataSource" }

        return Database.connect(dataSource, databaseConfig = databaseConfig)
    }
}
