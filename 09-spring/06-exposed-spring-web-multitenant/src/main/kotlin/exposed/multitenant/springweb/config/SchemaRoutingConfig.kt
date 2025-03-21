package exposed.multitenant.springweb.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.springweb.tenant.TenantAwareDataSource
import exposed.multitenant.springweb.tenant.Tenants
import exposed.multitenant.springweb.tenant.Tenants.Tenant
import io.bluetape4k.exceptions.NotSupportedException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.uninitialized
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
class SchemaRoutingConfig {

    companion object: KLogging()

    @Autowired
    private val environment: Environment = uninitialized()

    private fun getActiveProfile(default: String = "h2"): String {
        return environment.activeProfiles.firstOrNull() ?: default
    }

    private fun getHikariConfig(): HikariConfig {
        when (val profile = getActiveProfile()) {
            "h2" ->
                return HikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:multitenant;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
                    driverClassName = "org.h2.Driver"
                    username = "sa"
                    password = ""
                    maximumPoolSize = 5
                    isAutoCommit = false
                }
            "postgres" ->
                return with(PostgreSQLServer.Launcher.postgres) {
                    HikariConfig().also {
                        it.driverClassName = getDriverClassName()
                        it.jdbcUrl = getJdbcUrl()
                        it.username = getUsername()
                        it.password = getPassword()
                    }
                }
            else -> throw NotSupportedException("Unsupported profile: $profile")
        }
    }

    /**
     * Active Profile 에 해당하는 DataSource (H2, PostgreSQL) 를 입력받아, Tenant 별로 DataSource 를 설정합니다.
     */
    @Suppress("UNCHECKED_CAST")
    @Primary
    @Bean
    fun tenantAwareDataSource(): TenantAwareDataSource {
        val tenantDataSource = TenantAwareDataSource()

        val targetDataSources = Tenant.entries.associateWith {
            HikariDataSource(getHikariConfig())
        }

        tenantDataSource.setTargetDataSources(targetDataSources as Map<Any, Any>)
        tenantDataSource.setDefaultTargetDataSource(targetDataSources[Tenants.DEFAULT_TENANT] as Any)
        tenantDataSource.afterPropertiesSet()

        return tenantDataSource
    }

    @Bean
    fun exposedDatabaseConfig(): DatabaseConfig {
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
