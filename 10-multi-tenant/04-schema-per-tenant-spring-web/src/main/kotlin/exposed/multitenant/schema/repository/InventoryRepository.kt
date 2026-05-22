package exposed.multitenant.schema.repository

import exposed.multitenant.schema.domain.CreateInventoryItemRequest
import exposed.multitenant.schema.domain.InventoryItemRecord

interface InventoryRepository {
    fun findAll(): List<InventoryItemRecord>
    fun findBySku(sku: String): InventoryItemRecord?
    fun create(command: CreateInventoryItemRequest): InventoryItemRecord
    fun countBySku(sku: String): Long
}
