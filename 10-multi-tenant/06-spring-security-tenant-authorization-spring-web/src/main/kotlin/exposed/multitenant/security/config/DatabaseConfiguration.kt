package exposed.multitenant.security.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.security.tenant.TenantId
import exposed.multitenant.security.tenant.TenantTransaction
import io.bluetape4k.exposed.tenant.jdbc.TenantJdbcResourceRegistry
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.Serializable

@Configuration
@EnableConfigurationProperties(TenantDataSourceProperties::class)
class DatabaseConfiguration {

    @Bean(destroyMethod = "close")
    fun tenantDatabaseRegistry(properties: TenantDataSourceProperties): TenantJdbcResourceRegistry<TenantId> =
        properties.toTenantJdbcResourceRegistry()

    @Bean
    fun tenantTransaction(registry: TenantJdbcResourceRegistry<TenantId>): TenantTransaction =
        TenantTransaction(registry)
}

/**
 * Spring properties를 공용 tenant JDBC registry의 resource factory 계약으로 변환합니다.
 *
 * Hikari `DataSource`가 factory에서 반환된 뒤에는 provider가 `DataSource`와 Exposed
 * `Database`의 lifecycle을 소유하고, registry 종료 시 disposer를 호출합니다.
 */
internal fun TenantDataSourceProperties.toTenantJdbcResourceRegistry(): TenantJdbcResourceRegistry<TenantId> {
    val configured = tenants.mapKeys { (key, _) -> TenantId.fromHeader(key) }
    val missing = TenantId.entries.filterNot(configured::containsKey)
    require(missing.isEmpty()) {
        "Missing tenant datasource configuration: ${missing.joinToString { it.headerValue }}"
    }

    configured.forEach { (tenantId, jdbc) ->
        jdbc.validate("tenant-${tenantId.headerValue}")
    }

    return TenantJdbcResourceRegistry.create(
        tenants = TenantId.entries,
        dataSourceFactory = { tenantId ->
            configured.getValue(tenantId).toHikariDataSource("tenant-${tenantId.headerValue}")
        },
        disposeDataSource = { _, dataSource -> dataSource.close() },
    )
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
