package exposed.multitenant.schema.repository

import exposed.multitenant.schema.domain.CreateInventoryItemRequest
import exposed.multitenant.schema.domain.InventoryItemRecord
import exposed.multitenant.schema.domain.InventoryItems
import exposed.multitenant.schema.domain.toInventoryItemRecord
import exposed.multitenant.schema.tenant.TenantTransaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedInventoryRepository(
    private val tenantTransaction: TenantTransaction,
) : InventoryRepository {

    override fun findAll(): List<InventoryItemRecord> =
        tenantTransaction.execute(operation = "inventory.findAll") {
            InventoryItems
                .selectAll()
                .orderBy(InventoryItems.sku)
                .map { it.toInventoryItemRecord() }
        }

    override fun findBySku(sku: String): InventoryItemRecord? =
        tenantTransaction.execute(operation = "inventory.findBySku") {
            InventoryItems
                .selectAll()
                .where { InventoryItems.sku eq sku }
                .singleOrNull()
                ?.toInventoryItemRecord()
        }

    override fun create(command: CreateInventoryItemRequest): InventoryItemRecord =
        tenantTransaction.execute(operation = "inventory.create") {
            InventoryItems.insert {
                it[sku] = command.sku
                it[name] = command.name
                it[quantity] = command.quantity
            }
            InventoryItemRecord(
                sku = command.sku,
                name = command.name,
                quantity = command.quantity
            )
        }

    override fun countBySku(sku: String): Long =
        tenantTransaction.execute(operation = "inventory.countBySku") {
            InventoryItems
                .selectAll()
                .where { InventoryItems.sku eq sku }
                .count()
        }
}
