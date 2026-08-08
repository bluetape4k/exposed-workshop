package exposed.examples.ktor.cache.coroutines.repository

import exposed.examples.ktor.cache.coroutines.model.ProductResponse
import exposed.examples.ktor.cache.coroutines.persistence.Products
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.jdbc.caffeine.repository.AbstractSuspendedJdbcCaffeineRepository
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import java.util.concurrent.atomic.AtomicInteger

class SuspendingProductRepository(
    private val allowedSkus: Set<String>,
    config: LocalCacheConfig = LocalCacheConfig(
        keyPrefix = "ktor-products",
        maximumSize = 1_000,
        writeMode = CacheWriteMode.WRITE_THROUGH,
    ),
    private val assertOwner: () -> Unit = {},
) : AbstractSuspendedJdbcCaffeineRepository<String, ProductResponse>(config) {

    private val readCounter = AtomicInteger()

    override val table: IdTable<String> = Products

    val databaseReads: Int
        get() = readCounter.get()

    fun accepts(sku: String): Boolean = sku in allowedSkus

    override fun ResultRow.toEntity(): ProductResponse =
        ProductResponse(
            sku = this[Products.id].value,
            name = this[Products.name],
            version = this[Products.version],
            source = "database",
        )

    override fun UpdateStatement.updateEntity(entity: ProductResponse) {
        this[Products.name] = entity.name
        this[Products.version] = entity.version
    }

    override fun BatchInsertStatement.insertEntity(entity: ProductResponse) {
        this[Products.id] = EntityID(entity.sku, Products)
        this[Products.name] = entity.name
        this[Products.version] = entity.version
    }

    override fun extractId(entity: ProductResponse): String = entity.sku

    override suspend fun findByIdFromDb(id: String): ProductResponse? {
        assertOwner()
        check(accepts(id)) { "Unknown product key is not admitted: $id" }
        readCounter.incrementAndGet()
        return super.findByIdFromDb(id)
    }

    override suspend fun get(id: String): ProductResponse? {
        assertOwner()
        check(accepts(id)) { "Unknown product key is not admitted: $id" }
        return super.get(id)
    }

    override suspend fun put(id: String, entity: ProductResponse) {
        assertOwner()
        check(accepts(id)) { "Unknown product key is not admitted: $id" }
        super.put(id, entity)
    }

    override suspend fun invalidate(id: String) {
        assertOwner()
        if (accepts(id)) super.invalidate(id)
    }

    suspend fun nextVersion(sku: String): Int {
        assertOwner()
        check(accepts(sku)) { "Unknown product key is not admitted: $sku" }
        return suspendedTransactionAsync(Dispatchers.IO) {
            Products
                .selectAll()
                .where { Products.id eq sku }
                .singleOrNull()
                ?.get(Products.version)
                ?: 0
        }.await() + 1
    }
}
