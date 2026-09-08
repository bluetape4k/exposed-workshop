package exposed.multitenant.security.config

import exposed.multitenant.security.tenant.TenantId
import exposed.multitenant.security.tenant.TenantTransaction
import exposed.shared.tenant.jdbc.TenantJdbcSettings
import exposed.shared.tenant.jdbc.toTenantJdbcResourceRegistry as createTenantJdbcResourceRegistry
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
    return tenants.createTenantJdbcResourceRegistry(
        parseTenant = TenantId::fromHeader,
        expectedTenants = TenantId.entries,
        tenantName = TenantId::headerValue,
    )
}

@ConfigurationProperties(prefix = "app")
data class TenantDataSourceProperties(
    val tenants: Map<String, TenantJdbcSettings> = emptyMap(),
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
