package exposed.examples.routing.config

import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
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
            registry.contains("default:rw").shouldBeTrue()
            registry.contains("default:ro").shouldBeTrue()
            rw.jdbcUrl shouldBeEqualTo "jdbc:h2:mem:routing_default_rw"
            ro.jdbcUrl shouldBeEqualTo "jdbc:h2:mem:routing_default_rw"
        } finally {
            registry.close()
        }
    }

    @Test
    fun `routingKeyResolver는 설정된 default tenant를 사용한다`() {
        val properties = RoutingDataSourceProperties().apply {
            defaultTenant = "fallback-tenant"
        }

        val resolver = config.routingKeyResolver(properties)

        resolver.currentLookupKey() shouldBeEqualTo "fallback-tenant:rw"
    }

    @Test
    fun `dataSourceRegistry 종료 시 등록된 Hikari pool을 닫는다`() {
        val properties = RoutingDataSourceProperties().apply {
            defaultTenant = "default"
            tenants["default"] = TenantDataSourceProperties().apply {
                rw = dataSourceNode("jdbc:h2:mem:routing_close_default_rw")
                ro = dataSourceNode("jdbc:h2:mem:routing_close_default_ro")
            }
        }

        val registry = config.dataSourceRegistry(properties)
        val rw = registry.get("default:rw") as HikariDataSource
        val ro = registry.get("default:ro") as HikariDataSource

        registry.close()

        rw.isClosed.shouldBeTrue()
        ro.isClosed.shouldBeTrue()
        registry.keys().size shouldBeEqualTo 0
    }

    private fun dataSourceNode(url: String) =
        DataSourceNodeProperties().apply {
            this.url = url
            username = "sa"
            password = ""
            driverClassName = "org.h2.Driver"
        }
}
