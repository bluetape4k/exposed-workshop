package exposed.multitenant.database.repository

import exposed.multitenant.database.domain.CreateInventoryItemRequest
import exposed.multitenant.database.domain.InventoryItemRecord

interface InventoryRepository {
    fun findAll(): List<InventoryItemRecord>
    fun findBySku(sku: String): InventoryItemRecord?
    fun create(request: CreateInventoryItemRequest): InventoryItemRecord
}
