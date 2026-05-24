package exposed.examples.ktor.cache.coroutines.repository

import exposed.examples.ktor.cache.coroutines.model.ProductResponse
import exposed.examples.ktor.cache.coroutines.persistence.Products
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.concurrent.atomic.AtomicInteger

class SuspendingProductRepository(
    private val database: Database,
) {
    private val readCounter = AtomicInteger()

    val databaseReads: Int
        get() = readCounter.get()

    suspend fun find(sku: String): ProductResponse? {
        readCounter.incrementAndGet()
        return newSuspendedTransaction(Dispatchers.IO, database) {
            Products
                .selectAll()
                .where { Products.sku eq sku }
                .singleOrNull()
                ?.let {
                    ProductResponse(
                        sku = it[Products.sku],
                        name = it[Products.name],
                        version = it[Products.version],
                        source = "database",
                    )
                }
        }
    }

    suspend fun update(sku: String, name: String): ProductResponse =
        newSuspendedTransaction(Dispatchers.IO, database) {
            val nextVersion = (findCurrentVersion(sku) ?: 0) + 1
            Products.upsert {
                it[Products.sku] = sku
                it[Products.name] = name
                it[Products.version] = nextVersion
            }
            ProductResponse(
                sku = sku,
                name = name,
                version = nextVersion,
                source = "database",
            )
        }

    private fun findCurrentVersion(sku: String): Int? =
        Products
            .selectAll()
            .where { Products.sku eq sku }
            .singleOrNull()
            ?.get(Products.version)
}
