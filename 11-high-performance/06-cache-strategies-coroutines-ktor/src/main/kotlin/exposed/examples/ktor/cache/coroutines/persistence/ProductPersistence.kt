package exposed.examples.ktor.cache.coroutines.persistence

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Products: IdTable<String>("products") {
    override val id: Column<EntityID<String>> = varchar("sku", 40).entityId()
    val sku get() = id
    val name = varchar("name", 120)
    val version = integer("version")

    override val primaryKey = PrimaryKey(sku)
}

class ProductPersistence(
    private val database: Database,
) {
    fun initialize(): Set<String> = transaction(database) {
            SchemaUtils.create(Products)
            if (Products.selectAll().empty()) {
                Products.batchInsert(
                    listOf(
                        Triple("sku-1", "Coroutine cache primer", 1),
                        Triple("sku-2", "Suspending transaction guide", 1),
                    ),
                ) { product ->
                    this[Products.sku] = EntityID(product.first, Products)
                    this[Products.name] = product.second
                    this[Products.version] = product.third
                }
            }
            Products.selectAll().map { it[Products.id].value }.toSet()
    }
}
