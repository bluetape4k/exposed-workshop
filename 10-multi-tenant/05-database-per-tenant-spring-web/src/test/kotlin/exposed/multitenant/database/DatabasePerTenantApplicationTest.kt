package exposed.multitenant.database

import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.database.config.TenantDataSourceProperties
import exposed.multitenant.database.config.TenantJdbcProperties
import exposed.multitenant.database.domain.CreateInventoryItemRequest
import exposed.multitenant.database.domain.InventoryItemRecord
import exposed.multitenant.database.domain.InventoryItems
import exposed.multitenant.database.repository.InventoryRepository
import exposed.multitenant.database.tenant.TenantContext
import exposed.multitenant.database.tenant.TenantDatabaseRegistry
import exposed.multitenant.database.tenant.TenantFilter
import exposed.multitenant.database.tenant.TenantId
import exposed.multitenant.database.tenant.TenantTransaction
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@SpringBootTest(
    classes = [
        DatabasePerTenantApplication::class,
        DatabasePerTenantApplicationTest.TestFailureConfiguration::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureWebTestClient(timeout = "2m")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabasePerTenantApplicationTest(
    @param:Autowired private val client: WebTestClient,
    @param:Autowired private val registry: TenantDatabaseRegistry,
    @param:Autowired private val repository: InventoryRepository,
    @param:Autowired private val tenantTransaction: TenantTransaction,
) {

    @Test
    fun `valid tenants read isolated seed data from different databases`() {
        val acme = getInventory("acme", "ACME-ROUTER-001")
        val globex = getInventory("globex", "GLOBEX-DRONE-001")

        assertThat(acme.warehouse).isEqualTo("acme-east")
        assertThat(globex.warehouse).isEqualTo("globex-hub")

        client.get()
            .uri("/inventory/GLOBEX-DRONE-001")
            .header(TenantFilter.TENANT_HEADER, "acme")
            .exchange()
            .expectStatus().isNotFound

        client.get()
            .uri("/inventory/ACME-ROUTER-001")
            .header(TenantFilter.TENANT_HEADER, "globex")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `writes for one tenant are not visible to another tenant`() {
        val sku = "ACME-ONLY-${System.nanoTime()}"

        client.post()
            .uri("/inventory")
            .header(TenantFilter.TENANT_HEADER, "acme")
            .bodyValue(
                CreateInventoryItemRequest(
                    sku = sku,
                    name = "Acme Private Item",
                    quantity = 3,
                    warehouse = "acme-private",
                )
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody(InventoryItemRecord::class.java)
            .value { body ->
                val item = requireNotNull(body)
                assertThat(item.sku).isEqualTo(sku)
                assertThat(item.warehouse).isEqualTo("acme-private")
            }

        client.get()
            .uri("/inventory/$sku")
            .header(TenantFilter.TENANT_HEADER, "globex")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `missing tenant header returns stable bad request body`() {
        client.get()
            .uri("/inventory")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("MISSING_TENANT")
            .jsonPath("$.message").isEqualTo("${TenantFilter.TENANT_HEADER} header is required")
    }

    @Test
    fun `unknown tenant header returns stable not found body`() {
        client.get()
            .uri("/inventory")
            .header(TenantFilter.TENANT_HEADER, "initech")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.code").isEqualTo("UNKNOWN_TENANT")
            .jsonPath("$.message").isEqualTo("Unknown tenant")
    }

    @Test
    fun `tenant context is cleared after downstream failure`() {
        client.get()
            .uri("/inventory/_failure-after-tenant")
            .header(TenantFilter.TENANT_HEADER, "acme")
            .exchange()
            .expectStatus().is5xxServerError

        client.get()
            .uri("/inventory")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("MISSING_TENANT")
    }

    @Test
    fun `parallel alternating tenant requests do not leak thread local tenant`() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            val expectedWarehouses = List(20) { index ->
                if (index % 2 == 0) "acme-east" else "globex-hub"
            }
            val tasks = List(20) { index ->
                Callable {
                    if (index % 2 == 0) {
                        getInventory("acme", "ACME-ROUTER-001").warehouse
                    } else {
                        getInventory("globex", "GLOBEX-DRONE-001").warehouse
                    }
                }
            }

            val warehouses = executor.invokeAll(tasks).map { it.get() }

            assertThat(warehouses).containsExactlyElementsOf(expectedWarehouses)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `tenant filter clears context in servlet thread after downstream failure`() {
        val filter = TenantFilter()
        val request = tenantRequest("acme")
        val response = MockHttpServletResponse()

        assertThatThrownBy {
            filter.doFilter(
                request,
                response,
                FilterChain { _, _ ->
                    assertThat(TenantContext.current()).isEqualTo(TenantId.ACME)
                    error("downstream failure")
                }
            )
        }.hasMessage("downstream failure")

        assertThat(TenantContext.currentOrNull()).isNull()
    }

    @Test
    fun `tenant filter replaces and clears context for sequential requests on same servlet thread`() {
        val filter = TenantFilter()
        val observed = mutableListOf<TenantId>()

        listOf(
            "acme" to TenantId.ACME,
            "globex" to TenantId.GLOBEX,
        ).forEach { (header, expectedTenant) ->
            filter.doFilter(
                tenantRequest(header),
                MockHttpServletResponse(),
                FilterChain { _, _ ->
                    observed += TenantContext.current()
                    assertThat(TenantContext.current()).isEqualTo(expectedTenant)
                }
            )
            assertThat(TenantContext.currentOrNull()).isNull()
        }

        assertThat(observed).containsExactly(TenantId.ACME, TenantId.GLOBEX)
    }

    @Test
    fun `failing tenant transaction rolls back selected tenant only`() {
        val sku = "ROLLBACK-${System.nanoTime()}"

        assertThatThrownBy {
            TenantContext.withTenant(TenantId.ACME) {
                tenantTransaction.execute {
                    InventoryItems.insert {
                        it[InventoryItems.sku] = sku
                        it[name] = "Rollback Candidate"
                        it[quantity] = 99
                        it[warehouse] = "acme-rollback"
                    }
                    error("rollback requested")
                }
            }
        }.hasMessage("rollback requested")

        TenantContext.withTenant(TenantId.ACME) {
            assertThat(repository.findBySku(sku)).isNull()
        }
        TenantContext.withTenant(TenantId.GLOBEX) {
            assertThat(repository.findBySku("GLOBEX-DRONE-001")).isNotNull()
        }
    }

    @Test
    fun `bootstrap creates inventory table in every configured tenant database`() {
        registry.configuredTenants().forEach { tenantId ->
            val count = transaction(registry.databaseFor(tenantId)) {
                InventoryItems.selectAll().count()
            }

            assertThat(count).isGreaterThan(0)
        }
    }

    @Test
    fun `registry rejects missing known tenant configuration`() {
        assertThatThrownBy {
            TenantDatabaseRegistry.from(
                TenantDataSourceProperties(
                    tenants = mapOf("acme" to h2Properties("missing-known-acme")),
                )
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Missing tenant datasource configuration: globex")
    }

    @Test
    fun `registry rejects unknown tenant configuration`() {
        assertThatThrownBy {
            TenantDatabaseRegistry.from(
                TenantDataSourceProperties(
                    tenants = mapOf(
                        "acme" to h2Properties("unknown-acme"),
                        "globex" to h2Properties("unknown-globex"),
                        "initech" to h2Properties("unknown-initech"),
                    ),
                )
            )
        }.isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Unknown tenant: initech")
    }

    @Test
    fun `registry rejects h2 tenant database without close delay`() {
        assertThatThrownBy {
            TenantDatabaseRegistry.from(
                TenantDataSourceProperties(
                    tenants = mapOf(
                        "acme" to TenantJdbcProperties(
                            jdbcUrl = "jdbc:h2:mem:no_close_delay_acme;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
                        ),
                        "globex" to h2Properties("no-close-delay-globex"),
                    ),
                )
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("DB_CLOSE_DELAY=-1")
    }

    @Test
    fun `registry close closes owned hikari datasources`() {
        val standalone = TenantDatabaseRegistry.from(
            TenantDataSourceProperties(
                tenants = mapOf(
                    "acme" to h2Properties("close-acme"),
                    "globex" to h2Properties("close-globex"),
                ),
            )
        )
        val dataSources = TenantId.entries.map { tenantId ->
            standalone.dataSourceFor(tenantId) as HikariDataSource
        }

        standalone.close()

        assertThat(dataSources).allMatch { it.isClosed }
    }

    private fun getInventory(
        tenant: String,
        sku: String,
    ): InventoryItemRecord =
        client.get()
            .uri("/inventory/$sku")
            .header(TenantFilter.TENANT_HEADER, tenant)
            .exchange()
            .expectStatus().isOk
            .expectBody(InventoryItemRecord::class.java)
            .returnResult()
            .responseBody
            ?: error("Inventory item response body is missing")

    private fun h2Properties(name: String): TenantJdbcProperties =
        TenantJdbcProperties(
            jdbcUrl = "jdbc:h2:mem:$name;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        )

    private fun tenantRequest(tenant: String): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/inventory").apply {
            addHeader(TenantFilter.TENANT_HEADER, tenant)
        }

    @TestConfiguration
    class TestFailureConfiguration {
        @Bean
        fun failureAfterTenantController(): FailureAfterTenantController =
            FailureAfterTenantController()
    }

    @RestController
    class FailureAfterTenantController {
        @GetMapping("/inventory/_failure-after-tenant")
        fun fail(): Nothing {
            check(TenantContext.currentOrNull() == TenantId.ACME)
            error("failure after tenant resolution")
        }
    }
}
