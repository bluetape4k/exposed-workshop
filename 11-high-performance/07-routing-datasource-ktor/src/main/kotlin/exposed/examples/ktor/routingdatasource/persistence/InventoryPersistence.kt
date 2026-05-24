package exposed.examples.ktor.routingdatasource.persistence

import exposed.examples.ktor.routingdatasource.routing.DataSourceRole
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Inventory: Table("inventory") {
    val sku = varchar("sku", 40)
    val quantity = integer("quantity")

    override val primaryKey = PrimaryKey(sku)
}

class InventoryPersistence(
    private val database: Database,
) {
    fun initialize(role: DataSourceRole) {
        transaction(database) {
            SchemaUtils.create(Inventory)
            if (Inventory.selectAll().empty()) {
                Inventory.batchInsert(
                    listOf(
                        "sku-1" to if (role == DataSourceRole.READ) 10 else 100,
                        "sku-2" to if (role == DataSourceRole.READ) 20 else 200,
                    ),
                ) { row ->
                    this[Inventory.sku] = row.first
                    this[Inventory.quantity] = row.second
                }
            }
        }
    }
}
