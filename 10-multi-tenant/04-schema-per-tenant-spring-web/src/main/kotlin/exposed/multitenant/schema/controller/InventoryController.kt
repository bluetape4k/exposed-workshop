package exposed.multitenant.schema.controller

import exposed.multitenant.schema.domain.CreateInventoryItemRequest
import exposed.multitenant.schema.domain.InventoryItemRecord
import exposed.multitenant.schema.service.InventoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/inventory")
class InventoryController(
    private val inventoryService: InventoryService,
) {

    @GetMapping
    fun findAll(): List<InventoryItemRecord> =
        inventoryService.findAll()

    @GetMapping("/{sku}")
    fun findBySku(@PathVariable sku: String): ResponseEntity<InventoryItemRecord> =
        inventoryService.findBySku(sku)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateInventoryItemRequest): InventoryItemRecord =
        inventoryService.create(request)
}
