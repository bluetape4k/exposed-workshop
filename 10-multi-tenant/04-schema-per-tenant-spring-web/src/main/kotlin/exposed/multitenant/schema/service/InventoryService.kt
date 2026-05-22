package exposed.multitenant.schema.service

import exposed.multitenant.schema.domain.CreateInventoryItemRequest
import exposed.multitenant.schema.domain.InventoryItemRecord
import exposed.multitenant.schema.repository.InventoryRepository
import org.springframework.stereotype.Service

@Service
class InventoryService(
    private val repository: InventoryRepository,
) {

    fun findAll(): List<InventoryItemRecord> =
        repository.findAll()

    fun findBySku(sku: String): InventoryItemRecord? =
        repository.findBySku(sku)

    fun create(command: CreateInventoryItemRequest): InventoryItemRecord =
        repository.create(command)

    fun countBySku(sku: String): Long =
        repository.countBySku(sku)
}
