package exposed.multitenant.security.service

import exposed.multitenant.security.domain.CreateInventoryItemRequest
import exposed.multitenant.security.domain.InventoryItemRecord
import exposed.multitenant.security.repository.InventoryRepository
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
