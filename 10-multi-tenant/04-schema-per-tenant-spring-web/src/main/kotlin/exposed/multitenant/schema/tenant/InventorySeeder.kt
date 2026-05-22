package exposed.multitenant.schema.tenant

import exposed.multitenant.schema.domain.CreateInventoryItemRequest
import exposed.multitenant.schema.domain.InventoryItems
import exposed.multitenant.schema.repository.InventoryRepository
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class InventorySeeder(
    private val database: Database,
    private val inventoryRepository: InventoryRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        TenantId.validateSchemaNames()
        TenantId.entries.forEach { tenant ->
            transaction(database) {
                try {
                    SchemaUtils.createSchema(tenant.schema)
                    SchemaUtils.setSchema(tenant.schema)
                    SchemaUtils.create(InventoryItems)
                } finally {
                    SchemaUtils.setSchema(PUBLIC_SCHEMA)
                }
            }
            seedTenant(tenant)
        }
    }

    private fun seedTenant(tenant: TenantId) {
        TenantContext.withTenant(tenant) {
            seedItems[tenant].orEmpty().forEach { item ->
                if (inventoryRepository.countBySku(item.sku) == 0L) {
                    inventoryRepository.create(item)
                }
            }
        }
    }

    private companion object {
        val seedItems = mapOf(
            TenantId.ACME to listOf(
                CreateInventoryItemRequest("shared-widget", "Acme Shared Widget", 12),
                CreateInventoryItemRequest("acme-only", "Acme Private Component", 7),
            ),
            TenantId.GLOBEX to listOf(
                CreateInventoryItemRequest("shared-widget", "Globex Shared Widget", 44),
                CreateInventoryItemRequest("globex-only", "Globex Private Component", 9),
            )
        )

    }
}
