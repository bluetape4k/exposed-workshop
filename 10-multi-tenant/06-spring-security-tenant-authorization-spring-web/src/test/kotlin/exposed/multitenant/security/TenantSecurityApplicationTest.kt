package exposed.multitenant.security

import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.security.config.TenantDataSourceProperties
import exposed.multitenant.security.config.TenantJdbcProperties
import exposed.multitenant.security.domain.CreateInventoryItemRequest
import exposed.multitenant.security.domain.InventoryItemRecord
import exposed.multitenant.security.domain.InventoryItems
import exposed.multitenant.security.repository.InventoryRepository
import exposed.multitenant.security.security.CredentialSource
import exposed.multitenant.security.security.DemoApiKeyAuthenticationFilter
import exposed.multitenant.security.security.DemoAuthenticationToken
import exposed.multitenant.security.security.DemoSessionAuthenticationFilter
import exposed.multitenant.security.security.TenantAuthenticationResolver
import exposed.multitenant.security.security.TenantAuthorizationFilter
import exposed.multitenant.security.tenant.TenantContext
import exposed.multitenant.security.tenant.TenantDatabaseRegistry
import exposed.multitenant.security.tenant.TenantId
import exposed.multitenant.security.tenant.TenantRequest
import exposed.multitenant.security.tenant.TenantTransaction
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import jakarta.servlet.FilterChain
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootTest(
    classes = [
        TenantSecurityApplication::class,
        TenantSecurityApplicationTest.TestFailureConfiguration::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureWebTestClient(timeout = "2m")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantSecurityApplicationTest(
    @param:Autowired private val client: WebTestClient,
    @param:Autowired private val registry: TenantDatabaseRegistry,
    @param:Autowired private val repository: InventoryRepository,
    @param:Autowired private val tenantTransaction: TenantTransaction,
) {

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `jwt tenant claim can read matching tenant data`() {
        val acme = getInventory("acme", "ACME-ROUTER-001", "demo-acme-token")
        val globex = getInventory("globex", "GLOBEX-DRONE-001", "demo-globex-token")

        acme.warehouse shouldBeEqualTo "acme-east"
        globex.warehouse shouldBeEqualTo "globex-hub"
    }

    @Test
    fun `jwt tenant mismatch is forbidden`() {
        client.get()
            .uri("/inventory/ACME-ROUTER-001")
            .bearer("demo-globex-token")
            .tenant("acme")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `jwt without tenant claim is forbidden before selector validation`() {
        client.get()
            .uri("/inventory")
            .bearer("demo-no-tenant-token")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `jwt with unknown or malformed tenant claim is forbidden`() {
        listOf(
            "demo-unknown-tenant-token",
            "demo-malformed-tenant-token",
            "demo-non-string-tenant-token",
        ).forEach { token ->
            client.get()
                .uri("/inventory")
                .bearer(token)
                .tenant("acme")
                .exchange()
                .expectStatus().isForbidden
        }
    }

    @Test
    fun `missing and invalid authentication are rejected`() {
        client.get()
            .uri("/inventory")
            .tenant("acme")
            .exchange()
            .expectStatus().isUnauthorized

        client.get()
            .uri("/inventory")
            .bearer("not-a-demo-token")
            .tenant("acme")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `api key and demo session authenticate matching tenants`() {
        val acme = client.get()
            .uri("/inventory/ACME-ROUTER-001")
            .apiKey("demo-acme-key")
            .tenant("acme")
            .exchange()
            .expectStatus().isOk
            .expectBody(InventoryItemRecord::class.java)
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        val globex = client.get()
            .uri("/inventory/GLOBEX-DRONE-001")
            .demoSession("globex-session")
            .tenant("globex")
            .exchange()
            .expectStatus().isOk
            .expectBody(InventoryItemRecord::class.java)
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        acme.warehouse shouldBeEqualTo "acme-east"
        globex.warehouse shouldBeEqualTo "globex-hub"
    }

    @Test
    fun `invalid api key and demo session are unauthorized`() {
        client.get()
            .uri("/inventory")
            .apiKey("bad-key")
            .tenant("acme")
            .exchange()
            .expectStatus().isUnauthorized

        client.get()
            .uri("/inventory")
            .demoSession("bad-session")
            .tenant("acme")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `demo session tenant mismatch is forbidden`() {
        client.get()
            .uri("/inventory/GLOBEX-DRONE-001")
            .demoSession("acme-session")
            .tenant("globex")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `multiple supported credentials are rejected before authentication`() {
        client.get()
            .uri("/inventory")
            .bearer("demo-acme-token")
            .apiKey("demo-globex-key")
            .tenant("acme")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("CONFLICTING_CREDENTIALS")

        TenantContext.currentOrNull().shouldBeNull()
    }

    @Test
    fun `duplicate supported credential values are rejected before authentication`() {
        client.get()
            .uri("/inventory")
            .header(DemoApiKeyAuthenticationFilter.API_KEY_HEADER, "demo-acme-key", "demo-globex-key")
            .tenant("acme")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("CONFLICTING_CREDENTIALS")
    }

    @Test
    fun `basic authorization is ignored as unsupported when api key is present`() {
        client.get()
            .uri("/inventory/ACME-ROUTER-001")
            .header(HttpHeaders.AUTHORIZATION, "Basic ignored")
            .apiKey("demo-acme-key")
            .tenant("acme")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `health bypasses tenant and credential filters`() {
        client.get()
            .uri("/actuator/health")
            .bearer("demo-acme-token")
            .apiKey("demo-globex-key")
            .exchange()
            .expectStatus().isOk

        client.get()
            .uri("/actuator/health")
            .apiKey("bad-key")
            .demoSession("bad-session")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `writes for one tenant are not visible to another tenant`() {
        val sku = "ACME-ONLY-${System.nanoTime()}"

        client.post()
            .uri("/inventory")
            .header(HttpHeaders.AUTHORIZATION, "Bearer demo-acme-token")
            .header(TenantRequest.TENANT_HEADER, "acme")
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
            .value { body: InventoryItemRecord? ->
                val item = body.shouldNotBeNull()
                item.sku shouldBeEqualTo sku
                item.warehouse shouldBeEqualTo "acme-private"
            }

        client.get()
            .uri("/inventory/$sku")
            .bearer("demo-globex-token")
            .tenant("globex")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `missing tenant header returns stable bad request body for authenticated tenant`() {
        client.get()
            .uri("/inventory")
            .bearer("demo-acme-token")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("MISSING_TENANT")
            .jsonPath("$.message").isEqualTo("${TenantRequest.TENANT_HEADER} header is required")
    }

    @Test
    fun `malformed tenant headers return stable bad request body`() {
        listOf("", "ac me", "acme,globex", "x".repeat(TenantRequest.MAX_TENANT_HEADER_LENGTH + 1)).forEach { header ->
            client.get()
                .uri("/inventory")
                .bearer("demo-acme-token")
                .tenant(header)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("MISSING_TENANT")
        }

        client.get()
            .uri("/inventory")
            .bearer("demo-acme-token")
            .header(TenantRequest.TENANT_HEADER, "acme", "globex")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("MISSING_TENANT")
    }

    @Test
    fun `unknown tenant header returns stable not found body`() {
        client.get()
            .uri("/inventory")
            .bearer("demo-acme-token")
            .tenant("initech")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.code").isEqualTo("UNKNOWN_TENANT")
            .jsonPath("$.message").isEqualTo("Unknown tenant")
    }

    @Test
    fun `tenant normalization accepts surrounding whitespace and uppercase values`() {
        client.get()
            .uri("/inventory/ACME-ROUTER-001")
            .bearer("demo-acme-upper-token")
            .tenant("ACME")
            .exchange()
            .expectStatus().isOk
            .expectBody(InventoryItemRecord::class.java)
            .value { body ->
                body.shouldNotBeNull().warehouse shouldBeEqualTo "acme-east"
            }

        val filter = TenantAuthorizationFilter(TenantAuthenticationResolver())
        bindAuthentication(TenantId.ACME)
        filter.doFilter(
            tenantRequest(" ACME "),
            MockHttpServletResponse(),
            FilterChain { _, _ ->
                TenantContext.current() shouldBeEqualTo TenantId.ACME
            },
        )
        TenantContext.currentOrNull().shouldBeNull()
    }

    @Test
    fun `tenant context is cleared after downstream failure`() {
        client.get()
            .uri("/inventory/_failure-after-tenant")
            .bearer("demo-acme-token")
            .tenant("acme")
            .exchange()
            .expectStatus().is5xxServerError

        TenantContext.currentOrNull().shouldBeNull()
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
                        getInventory("acme", "ACME-ROUTER-001", "demo-acme-token").warehouse
                    } else {
                        getInventory("globex", "GLOBEX-DRONE-001", "demo-globex-token").warehouse
                    }
                }
            }

            val warehouses = executor.invokeAll(tasks).map { it.get() }

            warehouses shouldBeEqualTo expectedWarehouses
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `tenant authorization filter clears context in servlet thread after downstream failure`() {
        val filter = TenantAuthorizationFilter(TenantAuthenticationResolver())
        val request = tenantRequest("acme")
        val response = MockHttpServletResponse()
        bindAuthentication(TenantId.ACME)

        val failure = assertFailsWith<IllegalStateException> {
            filter.doFilter(
                request,
                response,
                FilterChain { _, _ ->
                    TenantContext.current() shouldBeEqualTo TenantId.ACME
                    error("downstream failure")
                },
            )
        }

        failure.message shouldBeEqualTo "downstream failure"
        TenantContext.currentOrNull().shouldBeNull()
    }

    @Test
    fun `tenant authorization filter replaces and clears context for sequential requests on same servlet thread`() {
        val filter = TenantAuthorizationFilter(TenantAuthenticationResolver())
        val observed = mutableListOf<TenantId>()

        listOf(
            "acme" to TenantId.ACME,
            "globex" to TenantId.GLOBEX,
        ).forEach { (header, expectedTenant) ->
            bindAuthentication(expectedTenant)
            filter.doFilter(
                tenantRequest(header),
                MockHttpServletResponse(),
                FilterChain { _, _ ->
                    observed += TenantContext.current()
                    TenantContext.current() shouldBeEqualTo expectedTenant
                },
            )
            TenantContext.currentOrNull().shouldBeNull()
        }

        observed shouldBeEqualTo listOf(TenantId.ACME, TenantId.GLOBEX)
    }

    @Test
    fun `failing tenant transaction rolls back selected tenant only`() {
        val sku = "ROLLBACK-${System.nanoTime()}"

        val failure = assertFailsWith<IllegalStateException> {
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
        }
        failure.message shouldBeEqualTo "rollback requested"

        TenantContext.withTenant(TenantId.ACME) {
            repository.findBySku(sku).shouldBeNull()
        }
        TenantContext.withTenant(TenantId.GLOBEX) {
            repository.findBySku("GLOBEX-DRONE-001").shouldNotBeNull()
        }
    }

    @Test
    fun `bootstrap creates inventory table in every configured tenant database`() {
        registry.configuredTenants().forEach { tenantId ->
            val count = transaction(registry.databaseFor(tenantId)) {
                InventoryItems.selectAll().count()
            }

            (count > 0) shouldBeEqualTo true
        }
    }

    @Test
    fun `registry rejects missing known tenant configuration`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            TenantDatabaseRegistry.from(
                TenantDataSourceProperties(
                    tenants = mapOf("acme" to h2Properties("missing-known-acme")),
                ),
            )
        }

        failure.message?.contains("Missing tenant datasource configuration: globex") shouldBeEqualTo true
    }

    @Test
    fun `registry rejects unknown tenant configuration`() {
        val failure = assertFailsWith<RuntimeException> {
            TenantDatabaseRegistry.from(
                TenantDataSourceProperties(
                    tenants = mapOf(
                        "acme" to h2Properties("unknown-acme"),
                        "globex" to h2Properties("unknown-globex"),
                        "initech" to h2Properties("unknown-initech"),
                    ),
                ),
            )
        }

        failure.message?.contains("Unknown tenant: initech") shouldBeEqualTo true
    }

    @Test
    fun `registry rejects h2 tenant database without close delay`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            TenantDatabaseRegistry.from(
                TenantDataSourceProperties(
                    tenants = mapOf(
                        "acme" to TenantJdbcProperties(
                            jdbcUrl = "jdbc:h2:mem:no_close_delay_acme;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
                        ),
                        "globex" to h2Properties("no-close-delay-globex"),
                    ),
                ),
            )
        }

        failure.message?.contains("DB_CLOSE_DELAY=-1") shouldBeEqualTo true
    }

    @Test
    fun `registry close closes owned hikari datasources`() {
        val standalone = TenantDatabaseRegistry.from(
            TenantDataSourceProperties(
                tenants = mapOf(
                    "acme" to h2Properties("close-acme"),
                    "globex" to h2Properties("close-globex"),
                ),
            ),
        )
        val dataSources = TenantId.entries.map { tenantId ->
            standalone.dataSourceFor(tenantId) as HikariDataSource
        }

        standalone.close()

        dataSources.all { it.isClosed } shouldBeEqualTo true
    }

    @Test
    fun `architecture keeps tenant context writes behind authorization filter`() {
        val productionFiles = Files.walk(moduleRoot.resolve("src/main/kotlin")).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }.toList()
        }

        val contextWriters = productionFiles
            .filterNot { it.fileName.toString() == "TenantContext.kt" }
            .filter { Files.readString(it).contains("TenantContext.set(") }
            .map { moduleRoot.relativize(it).toString() }

        contextWriters shouldBeEqualTo listOf(
            "src/main/kotlin/exposed/multitenant/security/security/TenantAuthorizationFilter.kt",
        )
    }

    @Test
    fun `architecture keeps copied module isolated from database package`() {
        val productionFiles = Files.walk(moduleRoot.resolve("src/main/kotlin")).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }.toList()
        }

        val violations = productionFiles
            .map { moduleRoot.relativize(it).toString() to Files.readString(it) }
            .filter { (_, source) -> source.contains("exposed.multitenant.database") }
            .map { (path, _) -> path }

        violations shouldBeEqualTo emptyList<String>()
    }

    @Test
    fun `architecture keeps repository transaction routing explicit`() {
        val repositoryFiles = Files.walk(moduleRoot.resolve("src/main/kotlin/exposed/multitenant/security/repository")).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }.toList()
        }

        val violations = repositoryFiles
            .map { moduleRoot.relativize(it).toString() to Files.readString(it) }
            .filter { (_, source) -> source.contains("transaction(") }
            .map { (path, _) -> path }

        violations shouldBeEqualTo emptyList<String>()
    }

    private fun getInventory(
        tenant: String,
        sku: String,
        token: String,
    ): InventoryItemRecord =
        client.get()
            .uri("/inventory/$sku")
            .bearer(token)
            .tenant(tenant)
            .exchange()
            .expectStatus().isOk
            .expectBody(InventoryItemRecord::class.java)
            .returnResult()
            .responseBody
            ?: error("Inventory item response body is missing")

    private fun WebTestClient.RequestHeadersSpec<*>.tenant(value: String): WebTestClient.RequestHeadersSpec<*> =
        header(TenantRequest.TENANT_HEADER, value)

    private fun WebTestClient.RequestHeadersSpec<*>.bearer(token: String): WebTestClient.RequestHeadersSpec<*> =
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")

    private fun WebTestClient.RequestHeadersSpec<*>.apiKey(value: String): WebTestClient.RequestHeadersSpec<*> =
        header(DemoApiKeyAuthenticationFilter.API_KEY_HEADER, value)

    private fun WebTestClient.RequestHeadersSpec<*>.demoSession(value: String): WebTestClient.RequestHeadersSpec<*> =
        header(DemoSessionAuthenticationFilter.SESSION_HEADER, value)

    private fun h2Properties(name: String): TenantJdbcProperties =
        TenantJdbcProperties(
            jdbcUrl = "jdbc:h2:mem:$name;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        )

    private fun tenantRequest(tenant: String): MockHttpServletRequest =
        MockHttpServletRequest("GET", "/inventory").apply {
            addHeader(TenantRequest.TENANT_HEADER, tenant)
        }

    private fun bindAuthentication(tenantId: TenantId) {
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = DemoAuthenticationToken(tenantId, CredentialSource.API_KEY)
        SecurityContextHolder.setContext(context)
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
            TenantContext.current() shouldBeEqualTo TenantId.ACME
            error("failure after tenant resolution")
        }
    }

    companion object {
        private val moduleRoot = Path.of("").toAbsolutePath()
    }
}
