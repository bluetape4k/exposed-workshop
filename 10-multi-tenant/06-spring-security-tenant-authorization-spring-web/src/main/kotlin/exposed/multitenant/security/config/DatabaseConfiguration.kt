package exposed.multitenant.security.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.security.tenant.TenantDatabaseRegistry
import exposed.multitenant.security.tenant.TenantTransaction
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.Serializable

@Configuration
@EnableConfigurationProperties(TenantDataSourceProperties::class)
class DatabaseConfiguration {

    @Bean(destroyMethod = "close")
    fun tenantDatabaseRegistry(properties: TenantDataSourceProperties): TenantDatabaseRegistry =
        TenantDatabaseRegistry.from(properties)

    @Bean
    fun tenantTransaction(registry: TenantDatabaseRegistry): TenantTransaction =
        TenantTransaction(registry)
}

@ConfigurationProperties(prefix = "app")
data class TenantDataSourceProperties(
    val tenants: Map<String, TenantJdbcProperties> = emptyMap(),
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class TenantJdbcProperties(
    val jdbcUrl: String = "",
    val username: String = "sa",
    val password: String = "",
    val driverClassName: String = "org.h2.Driver",
    val maximumPoolSize: Int? = null,
    val minimumIdle: Int? = null,
    val connectionTimeoutMs: Long? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    fun validate(poolName: String) {
        require(jdbcUrl.isNotBlank()) { "jdbcUrl must not be blank for $poolName" }
        require(!jdbcUrl.startsWith("jdbc:h2:", ignoreCase = true) || jdbcUrl.contains("DB_CLOSE_DELAY=-1")) {
            "H2 tenant URL must include DB_CLOSE_DELAY=-1 for $poolName"
        }
    }

    fun toHikariDataSource(poolName: String): HikariDataSource {
        validate(poolName)
        val config = HikariConfig().apply {
            this.poolName = poolName
            this.jdbcUrl = this@TenantJdbcProperties.jdbcUrl
            this.username = this@TenantJdbcProperties.username
            this.password = this@TenantJdbcProperties.password
            this.driverClassName = this@TenantJdbcProperties.driverClassName
            this.maximumPoolSize = this@TenantJdbcProperties.maximumPoolSize ?: 4
            this.minimumIdle = this@TenantJdbcProperties.minimumIdle ?: 1
            this.connectionTimeout = this@TenantJdbcProperties.connectionTimeoutMs ?: 5_000L
        }
        return HikariDataSource(config)
    }
}
