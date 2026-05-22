package exposed.multitenant.schema

import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.schema.domain.CreateInventoryItemRequest
import exposed.multitenant.schema.domain.InventoryItemRecord
import exposed.multitenant.schema.service.InventoryService
import exposed.multitenant.schema.tenant.ConnectionEvictor
import exposed.multitenant.schema.tenant.ConnectionUsageProbe
import exposed.multitenant.schema.tenant.ExposedTransactionRollbacker
import exposed.multitenant.schema.tenant.H2SchemaResetter
import exposed.multitenant.schema.tenant.SchemaResetter
import exposed.multitenant.schema.tenant.TenantContext
import exposed.multitenant.schema.tenant.TenantFilter
import exposed.multitenant.schema.tenant.TenantId
import exposed.multitenant.schema.tenant.TenantSchemaResetFailedException
import exposed.multitenant.schema.tenant.TenantTransaction
import exposed.multitenant.schema.tenant.TransactionRollbacker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.sql.DataSource
import kotlin.concurrent.withLock

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(
    classes = [SchemaPerTenantApplication::class, SchemaPerTenantApplicationTest.TestBeans::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaPerTenantApplicationTest(
    @param:Autowired private val client: WebTestClient,
    @param:Autowired private val inventoryService: InventoryService,
    @param:Autowired private val tenantTransaction: TenantTransaction,
    @param:Autowired private val connectionUsageProbe: TestConnectionUsageProbe,
    @param:Autowired private val resetter: TestSchemaResetter,
    @param:Autowired private val rollbacker: TestTransactionRollbacker,
    @param:Autowired private val evictor: TestConnectionEvictor,
    @param:Autowired private val dataSource: HikariDataSource,
) {

    @BeforeEach
    fun resetTestState() {
        TenantContext.clear()
        connectionUsageProbe.clear()
        resetter.reset()
        rollbacker.reset()
        evictor.reset()
    }

    @Test
    fun `valid tenants read isolated seed data through reused pool connection`() {
        val acme = client
            .get()
            .uri("/inventory/shared-widget")
            .header(TenantFilter.TENANT_HEADER, TenantId.ACME.headerValue)
            .exchange()
            .expectStatus().isOk
            .expectBody(InventoryItemRecord::class.java)
            .returnResult().responseBody

        val globex = client
            .get()
            .uri("/inventory/shared-widget")
            .header(TenantFilter.TENANT_HEADER, TenantId.GLOBEX.headerValue)
            .exchange()
            .expectStatus().isOk
            .expectBody(InventoryItemRecord::class.java)
            .returnResult().responseBody

        assertThat(acme?.name).isEqualTo("Acme Shared Widget")
        assertThat(acme?.quantity).isEqualTo(12)
        assertThat(globex?.name).isEqualTo("Globex Shared Widget")
        assertThat(globex?.quantity).isEqualTo(44)
        assertThat(connectionUsageProbe.snapshot()).allSatisfy {
            assertThat(it.sessionId).isNotNull()
        }
        assertThat(connectionUsageProbe.snapshot().map { it.sessionId }.toSet()).hasSize(1)
        assertThat(dataSource.hikariPoolMXBean.totalConnections).isEqualTo(1)
        assertThat(currentSchema()).isEqualTo("PUBLIC")
    }

    @Test
    fun `tenant local insert is not visible from another tenant`() {
        val sku = "acme-only-${System.nanoTime()}"

        client
            .post()
            .uri("/inventory")
            .header(TenantFilter.TENANT_HEADER, TenantId.ACME.headerValue)
            .bodyValue(CreateInventoryItemRequest(sku, "Tenant Local Item", 5))
            .exchange()
            .expectStatus().isCreated

        client
            .get()
            .uri("/inventory/$sku")
            .header(TenantFilter.TENANT_HEADER, TenantId.GLOBEX.headerValue)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `missing tenant header returns bad request`() {
        client
            .get()
            .uri("/inventory")
            .exchange()
            .expectStatus().isBadRequest
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "ACME",
            "acme,globex",
            "../../etc/passwd",
            "acme;DROP SCHEMA TENANT_GLOBEX",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        ]
    )
    fun `invalid tenant values return bad request`(tenantHeader: String) {
        client
            .get()
            .uri("/inventory")
            .header(TenantFilter.TENANT_HEADER, tenantHeader)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `tenant context is cleared after downstream failure`() {
        client
            .get()
            .uri("/inventory/_failure-after-tenant")
            .header(TenantFilter.TENANT_HEADER, TenantId.ACME.headerValue)
            .exchange()
            .expectStatus().is5xxServerError

        client
            .get()
            .uri("/inventory")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `reset failure after successful work rolls back and evicts connection`() {
        val sku = "reset-failure-${System.nanoTime()}"
        resetter.failNextReset()

        val thrown = catchThrowable {
            TenantContext.withTenant(TenantId.ACME) {
                inventoryService.create(CreateInventoryItemRequest(sku, "Rolled Back", 1))
            }
        }

        assertThat(thrown).isInstanceOf(TenantSchemaResetFailedException::class.java)
        assertThat(evictor.evictedConnections).isNotEmpty
        assertThat(dataSource.hikariPoolMXBean.activeConnections).isZero()

        TenantContext.withTenant(TenantId.ACME) {
            assertThat(inventoryService.findBySku(sku)).isNull()
        }
    }

    @Test
    fun `reset failure returns service unavailable and pool recovers for next request`() {
        val sku = "http-reset-failure-${System.nanoTime()}"
        resetter.failNextReset()

        client
            .post()
            .uri("/inventory")
            .header(TenantFilter.TENANT_HEADER, TenantId.ACME.headerValue)
            .bodyValue(CreateInventoryItemRequest(sku, "HTTP Reset Failure", 1))
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.error").isEqualTo("schema_reset_failed")

        assertThat(evictor.evictedConnections).isNotEmpty

        client
            .get()
            .uri("/inventory/shared-widget")
            .header(TenantFilter.TENANT_HEADER, TenantId.GLOBEX.headerValue)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.name").isEqualTo("Globex Shared Widget")
    }

    @Test
    fun `block failure remains primary when reset also fails`() {
        resetter.failNextReset()

        val thrown = catchThrowable {
            TenantContext.withTenant(TenantId.ACME) {
                tenantTransaction.execute(operation = "test.blockFailure") {
                    throw IllegalStateException("business failure")
                }
            }
        }

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown).hasMessage("business failure")
        assertThat(thrown.suppressed).anySatisfy {
            assertThat(it).hasMessage("test reset failure")
        }
        assertThat(evictor.evictedConnections).isNotEmpty
    }

    @Test
    fun `reset failure remains primary when connection eviction also fails`() {
        val sku = "reset-and-evict-failure-${System.nanoTime()}"
        resetter.failNextReset()
        evictor.failNextEvict()

        val thrown = catchThrowable {
            TenantContext.withTenant(TenantId.ACME) {
                inventoryService.create(CreateInventoryItemRequest(sku, "Eviction Also Failed", 1))
            }
        }

        assertThat(thrown).isInstanceOf(TenantSchemaResetFailedException::class.java)
        assertThat(thrown.cause).hasMessage("test reset failure")
        assertThat(thrown.cause?.suppressed).anySatisfy {
            assertThat(it).hasMessage("test eviction failure")
        }
    }

    @Test
    fun `reset failure remains primary when rollback also fails`() {
        val sku = "reset-and-rollback-failure-${System.nanoTime()}"
        resetter.failNextReset()
        rollbacker.failNextRollback()

        val thrown = catchThrowable {
            TenantContext.withTenant(TenantId.ACME) {
                inventoryService.create(CreateInventoryItemRequest(sku, "Rollback Also Failed", 1))
            }
        }

        assertThat(thrown).isInstanceOf(TenantSchemaResetFailedException::class.java)
        assertThat(thrown.cause).hasMessage("test reset failure")
        assertThat(thrown.cause?.suppressed).anySatisfy {
            assertThat(it).hasMessage("test rollback failure")
        }
        assertThat(evictor.evictedConnections).isNotEmpty
    }

    @TestConfiguration
    class TestBeans {
        @Bean
        @Primary
        fun testSchemaResetter(): TestSchemaResetter =
            TestSchemaResetter()

        @Bean
        @Primary
        fun testTransactionRollbacker(): TestTransactionRollbacker =
            TestTransactionRollbacker()

        @Bean
        @Primary
        fun testConnectionUsageProbe(): TestConnectionUsageProbe =
            TestConnectionUsageProbe()

        @Bean
        @Primary
        fun testConnectionEvictor(dataSource: DataSource): TestConnectionEvictor =
            TestConnectionEvictor(dataSource as HikariDataSource)

        @Bean
        fun failureAfterTenantController(): FailureAfterTenantController =
            FailureAfterTenantController()
    }

    private fun currentSchema(): String =
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT SCHEMA()").use { resultSet ->
                    resultSet.next()
                    resultSet.getString(1)
                }
            }
        }
}

data class ConnectionUsage(
    val operation: String,
    val connectionIdentity: Int,
    val sessionId: Long?,
)

class TestConnectionUsageProbe : ConnectionUsageProbe {
    private val records = mutableListOf<ConnectionUsage>()
    private val lock = ReentrantLock()

    override fun record(operation: String, connection: Connection) {
        val sessionId = currentSessionId(connection)
        lock.withLock {
            records += ConnectionUsage(operation, System.identityHashCode(connection), sessionId)
        }
    }

    fun snapshot(): List<ConnectionUsage> =
        lock.withLock {
            records.toList()
        }

    fun clear() {
        lock.withLock {
            records.clear()
        }
    }

    private fun currentSessionId(connection: Connection): Long? =
        try {
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT SESSION_ID()").use { resultSet ->
                    resultSet.next()
                    resultSet.getLong(1)
                }
            }
        } catch (_: SQLException) {
            null
        }
}

class TestSchemaResetter : SchemaResetter {
    private val delegate = H2SchemaResetter()
    private var failNext = false

    fun failNextReset() {
        failNext = true
    }

    fun reset() {
        failNext = false
    }

    override fun resetToPublic() {
        if (failNext) {
            failNext = false
            throw IllegalStateException("test reset failure")
        }
        delegate.resetToPublic()
    }
}

class TestConnectionEvictor(
    private val dataSource: HikariDataSource,
) : ConnectionEvictor {
    val evictedConnections = CopyOnWriteArrayList<Int>()
    private val failNext = AtomicBoolean(false)

    fun reset() {
        evictedConnections.clear()
        failNext.set(false)
    }

    fun failNextEvict() {
        failNext.set(true)
    }

    override fun evict(connection: Connection) {
        if (failNext.compareAndSet(true, false)) {
            throw IllegalStateException("test eviction failure")
        }
        evictedConnections += System.identityHashCode(connection)
        dataSource.evictConnection(connection)
    }
}

class TestTransactionRollbacker : TransactionRollbacker {
    private val delegate = ExposedTransactionRollbacker()
    private val failNext = AtomicBoolean(false)

    fun reset() {
        failNext.set(false)
    }

    fun failNextRollback() {
        failNext.set(true)
    }

    override fun rollbackCurrent() {
        if (failNext.compareAndSet(true, false)) {
            throw IllegalStateException("test rollback failure")
        }
        delegate.rollbackCurrent()
    }
}

@RestController
@RequestMapping("/inventory")
class FailureAfterTenantController {
    @GetMapping("/_failure-after-tenant")
    fun failureAfterTenantResolution(): Nothing =
        throw IllegalStateException("failure after tenant resolution")
}
