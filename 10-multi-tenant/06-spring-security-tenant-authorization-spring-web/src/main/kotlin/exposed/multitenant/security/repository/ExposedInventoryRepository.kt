package exposed.multitenant.security.repository

import exposed.multitenant.security.domain.CreateInventoryItemRequest
import exposed.multitenant.security.domain.InventoryItemRecord
import exposed.multitenant.security.domain.InventoryItems
import exposed.multitenant.security.domain.toInventoryItemRecord
import exposed.multitenant.security.tenant.TenantTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedInventoryRepository(
    private val tenantTransaction: TenantTransaction,
) : InventoryRepository {

    override fun findAll(): List<InventoryItemRecord> =
        tenantTransaction.execute {
            InventoryItems
                .selectAll()
                .orderBy(InventoryItems.sku to SortOrder.ASC)
                .map(ResultRow::toInventoryItemRecord)
        }

    override fun findBySku(sku: String): InventoryItemRecord? =
        tenantTransaction.execute {
            InventoryItems
                .selectAll()
                .where { InventoryItems.sku eq sku }
                .singleOrNull()
                ?.toInventoryItemRecord()
        }

    override fun create(request: CreateInventoryItemRequest): InventoryItemRecord =
        tenantTransaction.execute {
            insertAndReturn(request)
        }

    private fun insertAndReturn(request: CreateInventoryItemRequest): InventoryItemRecord {
        InventoryItems.insert {
            it[sku] = request.sku
            it[name] = request.name
            it[quantity] = request.quantity
            it[warehouse] = request.warehouse
        }
        return requireNotNull(
            InventoryItems
                .selectAll()
                .where { InventoryItems.sku eq request.sku }
                .singleOrNull()
                ?.toInventoryItemRecord()
        ) {
            "Inserted inventory item was not found. sku=${request.sku}"
        }
    }
}
