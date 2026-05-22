package exposed.multitenant.security.tenant

import exposed.multitenant.security.domain.CreateInventoryItemRequest
import exposed.multitenant.security.domain.InventoryItems
import exposed.multitenant.security.repository.InventoryRepository
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class InventorySeeder(
    private val registry: TenantDatabaseRegistry,
    private val repository: InventoryRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        registry.configuredTenants().forEach { tenantId ->
            bootstrapTenantDatabase(tenantId)
            seedTenant(tenantId)
        }
    }

    private fun bootstrapTenantDatabase(tenantId: TenantId) {
        transaction(registry.databaseFor(tenantId)) {
            SchemaUtils.create(InventoryItems)
        }
    }

    private fun seedTenant(tenantId: TenantId) {
        val seed = seedRows[tenantId].orEmpty()
        seed.forEach { request ->
            TenantContext.withTenant(tenantId) {
                if (repository.findBySku(request.sku) == null) {
                    repository.create(request)
                }
            }
        }
    }

    companion object {
        private val seedRows = mapOf(
            TenantId.ACME to listOf(
                CreateInventoryItemRequest(
                    sku = "ACME-ROUTER-001",
                    name = "Acme Edge Router",
                    quantity = 12,
                    warehouse = "acme-east",
                ),
                CreateInventoryItemRequest(
                    sku = "ACME-SENSOR-002",
                    name = "Acme Floor Sensor",
                    quantity = 36,
                    warehouse = "acme-west",
                ),
            ),
            TenantId.GLOBEX to listOf(
                CreateInventoryItemRequest(
                    sku = "GLOBEX-DRONE-001",
                    name = "Globex Survey Drone",
                    quantity = 8,
                    warehouse = "globex-hub",
                ),
                CreateInventoryItemRequest(
                    sku = "GLOBEX-GATEWAY-002",
                    name = "Globex IoT Gateway",
                    quantity = 18,
                    warehouse = "globex-hub",
                ),
            ),
        )
    }
}
