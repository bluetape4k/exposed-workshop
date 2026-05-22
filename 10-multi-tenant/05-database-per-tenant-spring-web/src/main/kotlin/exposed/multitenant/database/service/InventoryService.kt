package exposed.multitenant.database.service

import exposed.multitenant.database.domain.CreateInventoryItemRequest
import exposed.multitenant.database.domain.InventoryItemRecord
import exposed.multitenant.database.repository.InventoryRepository
import org.springframework.stereotype.Service

@Service
class InventoryService(
    private val repository: InventoryRepository,
) {

    fun list(): List<InventoryItemRecord> =
        repository.findAll()

    fun findBySku(sku: String): InventoryItemRecord? =
        repository.findBySku(sku)

    fun create(request: CreateInventoryItemRequest): InventoryItemRecord =
        repository.create(request)
}
