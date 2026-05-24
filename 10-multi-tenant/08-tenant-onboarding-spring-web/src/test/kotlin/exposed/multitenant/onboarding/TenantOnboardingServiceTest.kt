package exposed.multitenant.onboarding

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.onboarding.model.OnboardTenantCommand
import exposed.multitenant.onboarding.model.TenantStatus
import exposed.multitenant.onboarding.service.DuplicateTenantException
import exposed.multitenant.onboarding.service.TenantOnboardingService
import exposed.multitenant.onboarding.service.TenantProvisioningException
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantOnboardingServiceTest {

    private lateinit var dataSource: HikariDataSource
    private lateinit var service: TenantOnboardingService

    @BeforeEach
    fun setUp() {
        dataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:tenant_onboarding_test_${sequence.incrementAndGet()};DB_CLOSE_DELAY=-1"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                maximumPoolSize = 3
                isAutoCommit = false
            },
        )
        service = TenantOnboardingService(Database.connect(dataSource)).also {
            it.initializeCatalog()
        }
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `onboards tenant and provisions schema resources`() {
        val record = service.onboard(OnboardTenantCommand("acme-01", "ACME"))

        record.tenantId shouldBeEqualTo "acme-01"
        record.schemaName shouldBeEqualTo "TENANT_ACME_01"
        record.status shouldBeEqualTo TenantStatus.ACTIVE
        service.schemaExists(record.schemaName) shouldBeEqualTo true
        service.markerTableExists(record.schemaName) shouldBeEqualTo true
        service.find("acme-01") shouldBeEqualTo record
    }

    @Test
    fun `rejects duplicate tenant onboarding`() {
        service.onboard(OnboardTenantCommand("acme-01", "ACME"))

        assertFailsWith<DuplicateTenantException> {
            service.onboard(OnboardTenantCommand("acme-01", "ACME duplicate"))
        }
    }

    @Test
    fun `cleans up schema when provisioning fails`() {
        val failure = assertFailsWith<TenantProvisioningException> {
            service.onboard(
                OnboardTenantCommand(
                    tenantId = "broken-01",
                    displayName = "Broken",
                    failAfterSchemaCreation = true,
                ),
            )
        }

        failure.message shouldBeEqualTo "Simulated provisioning failure for broken-01"
        service.find("broken-01") shouldBeEqualTo null
        service.schemaExists("TENANT_BROKEN_01") shouldBeEqualTo false
    }

    companion object {
        private val sequence = AtomicInteger()
    }
}
