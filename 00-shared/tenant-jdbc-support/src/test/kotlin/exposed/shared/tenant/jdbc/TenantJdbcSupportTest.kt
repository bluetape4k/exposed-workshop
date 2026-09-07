package exposed.shared.tenant.jdbc

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.Test

class TenantJdbcSupportTest {

    @Test
    fun `normalization rejects duplicate tenant keys after trimming and case folding`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            linkedMapOf(
                "acme" to h2Settings("duplicate-acme"),
                " ACME " to h2Settings("duplicate-acme-normalized"),
            ).normalizeTenantJdbcSettings(
                parseTenant = { it.trim().lowercase() },
                expectedTenants = listOf("acme"),
                tenantName = { it },
            )
        }

        assertEquals("Duplicate tenant datasource configuration: acme", failure.message)
    }

    @Test
    fun `normalization rejects missing expected tenant keys`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            mapOf("acme" to h2Settings("missing-globex")).normalizeTenantJdbcSettings(
                parseTenant = { it.trim().lowercase() },
                expectedTenants = listOf("acme", "globex"),
                tenantName = { it },
            )
        }

        assertEquals("Missing tenant datasource configuration: globex", failure.message)
    }

    @Test
    fun `settings validate h2 lifecycle and create configured hikari datasource`() {
        val dataSource = h2Settings("factory").toHikariDataSource("tenant-acme")
        try {
            assertEquals("tenant-acme", dataSource.poolName)
            assertEquals(4, dataSource.maximumPoolSize)
            assertEquals(1, dataSource.minimumIdle)
        } finally {
            dataSource.close()
        }
    }

    @Test
    fun `registry factory validates settings and owns datasource lifecycle`() {
        val registry = linkedMapOf(
            "acme" to h2Settings("registry-acme"),
            " ACME " to h2Settings("registry-acme-duplicate"),
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            registry.toTenantJdbcResourceRegistry(
                parseTenant = { it.trim().lowercase() },
                expectedTenants = listOf("acme"),
                tenantName = { it },
            )
        }

        assertEquals("Duplicate tenant datasource configuration: acme", failure.message)

        val resources = mapOf(
            "acme" to h2Settings("registry-acme-valid"),
            "globex" to h2Settings("registry-globex-valid"),
        ).toTenantJdbcResourceRegistry(
            parseTenant = { it.trim().lowercase() },
            expectedTenants = listOf("acme", "globex"),
            tenantName = { it },
        )
        val dataSources = listOf("acme", "globex").map { resources.dataSourceFor(it) as HikariDataSource }

        resources.close()

        assertEquals(true, dataSources.all(HikariDataSource::isClosed))
    }

    private fun h2Settings(name: String): TenantJdbcSettings =
        TenantJdbcSettings(
            jdbcUrl = "jdbc:h2:mem:$name;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        )
}
