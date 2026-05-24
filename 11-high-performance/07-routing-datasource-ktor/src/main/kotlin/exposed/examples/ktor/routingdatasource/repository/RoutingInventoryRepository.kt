package exposed.examples.ktor.routingdatasource.repository

import exposed.examples.ktor.routingdatasource.model.InventoryResponse
import exposed.examples.ktor.routingdatasource.model.RoutingStatsResponse
import exposed.examples.ktor.routingdatasource.persistence.Inventory
import exposed.examples.ktor.routingdatasource.routing.DataSourceRole
import exposed.examples.ktor.routingdatasource.routing.RoutingContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.concurrent.atomic.AtomicInteger

class RoutingInventoryRepository(
    private val databases: Map<DataSourceRole, Database>,
) {
    private val readSelections = AtomicInteger()
    private val writeSelections = AtomicInteger()

    fun find(sku: String): InventoryResponse =
        withSelectedDatabase { role, database ->
            transaction(database) {
                Inventory
                    .selectAll()
                    .where { Inventory.sku eq sku }
                    .singleOrNull()
                    ?.let {
                        InventoryResponse(
                            sku = it[Inventory.sku],
                            quantity = it[Inventory.quantity],
                            selectedDataSource = role,
                        )
                    }
                    ?: throw IllegalArgumentException("Unknown sku: $sku")
            }
        }

    fun update(sku: String, quantity: Int): InventoryResponse =
        withSelectedDatabase { role, database ->
            require(role == DataSourceRole.WRITE) { "Inventory updates must use WRITE datasource" }
            require(quantity >= 0) { "quantity must be zero or positive" }
            transaction(database) {
                Inventory.upsert {
                    it[Inventory.sku] = sku
                    it[Inventory.quantity] = quantity
                }
            }
            InventoryResponse(sku = sku, quantity = quantity, selectedDataSource = role)
        }

    fun stats(): RoutingStatsResponse =
        RoutingStatsResponse(
            readSelections = readSelections.get(),
            writeSelections = writeSelections.get(),
        )

    private fun <T> withSelectedDatabase(block: (DataSourceRole, Database) -> T): T {
        val role = RoutingContext.currentRole()
        if (role == DataSourceRole.READ) {
            readSelections.incrementAndGet()
        } else {
            writeSelections.incrementAndGet()
        }
        val database = databases[role] ?: throw IllegalStateException("No database configured for $role")
        return block(role, database)
    }
}
