package exposed.multitenant.security.repository

import exposed.multitenant.security.domain.CreateInventoryItemRequest
import exposed.multitenant.security.domain.InventoryItemRecord

interface InventoryRepository {
    fun findAll(): List<InventoryItemRecord>
    fun findBySku(sku: String): InventoryItemRecord?
    fun create(request: CreateInventoryItemRequest): InventoryItemRecord
}
