package exposed.examples.routing.config

import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoutingDataSourceConfigTest {

    private val config = RoutingDataSourceConfig()

    @Test
    fun `read-only 설정이 없어도 rw 설정으로 ro 키를 등록한다`() {
        val properties = RoutingDataSourceProperties().apply {
            defaultTenant = "default"
            tenants["default"] = TenantDataSourceProperties().apply {
                rw = dataSourceNode("jdbc:h2:mem:routing_default_rw")
            }
        }

        val registry = config.dataSourceRegistry(properties)

        val rw = registry.get("default:rw") as HikariDataSource
        val ro = registry.get("default:ro") as HikariDataSource

        try {
            assertTrue(registry.contains("default:rw"))
            assertTrue(registry.contains("default:ro"))
            assertEquals("jdbc:h2:mem:routing_default_rw", rw.jdbcUrl)
            assertEquals("jdbc:h2:mem:routing_default_rw", ro.jdbcUrl)
        } finally {
            rw.close()
            ro.close()
        }
    }

    @Test
    fun `routingKeyResolver는 설정된 default tenant를 사용한다`() {
        val properties = RoutingDataSourceProperties().apply {
            defaultTenant = "fallback-tenant"
        }

        val resolver = config.routingKeyResolver(properties)

        assertEquals("fallback-tenant:rw", resolver.currentLookupKey())
    }

    private fun dataSourceNode(url: String) =
        DataSourceNodeProperties().apply {
            this.url = url
            username = "sa"
            password = ""
            driverClassName = "org.h2.Driver"
        }
}
