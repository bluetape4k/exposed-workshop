package exposed.multitenant.schema.domain

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table

object InventoryItems : Table("inventory_items") {
    val sku = varchar("sku", 64)
    val name = varchar("name", 120)
    val quantity = integer("quantity")

    override val primaryKey = PrimaryKey(sku)
}

fun ResultRow.toInventoryItemRecord(): InventoryItemRecord =
    InventoryItemRecord(
        sku = this[InventoryItems.sku],
        name = this[InventoryItems.name],
        quantity = this[InventoryItems.quantity]
    )
