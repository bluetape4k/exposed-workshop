package exposed.multitenant.webflux.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.webflux.tenant.TenantAwareDataSource
import exposed.multitenant.webflux.tenant.Tenants
import io.bluetape4k.exceptions.NotSupportedException
import io.bluetape4k.logging.coroutines.KLoggingChannel
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
import javax.sql.DataSource

/**
 * Exposed 관련 설정입니다. 여기서는 예제용으로 프로파일 기준으로 DB를 설정합니다.
 *
 * 일반적으로는 Exposed의 Spring Boot 용 AutoConfiguration을 사용하면 됩니다.
 */
@Configuration
class ExposedMultitenantConfig {

    companion object: KLoggingChannel()

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
     * [TenantAwareDataSource] 는 `Database per Tenant` 방식으로 멀티 테넌시를 지원합니다.
     * Active Profile 에 해당하는 DataSource (H2, PostgreSQL) 를 입력받아, Tenant 별로 DataSource 를 설정합니다.
     */
    @Suppress("UNCHECKED_CAST")
    @Bean
    fun tenantAwareDataSource(): TenantAwareDataSource {
        val tenantDataSource = TenantAwareDataSource()

        val targetDataSources = Tenants.Tenant.entries.associateWith {
            // 여기에서 Database 경로를 변경해주어야 합니다.
            HikariDataSource(getHikariConfig())
        }

        tenantDataSource.setTargetDataSources(targetDataSources as Map<Any, Any>)
        tenantDataSource.setDefaultTargetDataSource(targetDataSources[Tenants.DEFAULT_TENANT] as Any)
        tenantDataSource.afterPropertiesSet()

        return tenantDataSource
    }

    /**
     * 하나의 Shared Database 를 사용하여, Separate Schema 방식으로 멀티 테넌시를 지원합니다.
     */
    @Primary
    @Bean
    fun dataSource(): DataSource {
        val config = getHikariConfig()
        return HikariDataSource(config)
    }

    @Bean
    fun database(dataSource: DataSource, databaseConfig: DatabaseConfig): Database {
        log.info { "Database connection: $dataSource" }
        return Database.connect(dataSource)
    }

    @Bean
    fun exposedDatabaseConfig(): DatabaseConfig {
        return DatabaseConfig {
            maxEntitiesToStoreInCachePerEntity = 100
            useNestedTransactions = true
        }
    }
}
