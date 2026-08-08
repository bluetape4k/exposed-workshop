package exposed.examples.ktor.cache.coroutines.service

import exposed.examples.ktor.cache.coroutines.model.CoroutineCacheStatsResponse
import exposed.examples.ktor.cache.coroutines.model.ProductResponse
import exposed.examples.ktor.cache.coroutines.model.UpdateProductRequest
import exposed.examples.ktor.cache.coroutines.repository.SuspendingProductRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class CoroutineCachedProductService(
    private val repository: SuspendingProductRepository,
    private val writeFailureLatch: AtomicBoolean = AtomicBoolean(false),
) {
    private val inFlightLoads = AtomicInteger()
    private val cacheHits = AtomicInteger()
    private val cacheMisses = AtomicInteger()

    suspend fun readThrough(sku: String): ProductResponse {
        check(repository.accepts(sku)) { "Unknown product key is not admitted: $sku" }
        val cacheHit = repository.cache.getIfPresent(sku) != null
        if (cacheHit) {
            cacheHits.incrementAndGet()
        } else {
            cacheMisses.incrementAndGet()
        }

        inFlightLoads.incrementAndGet()
        try {
            val product = repository.get(sku) ?: throw IllegalArgumentException("Unknown product: $sku")
            return product.copy(source = if (cacheHit) "read-through-cache" else "read-through-database")
        } finally {
            inFlightLoads.decrementAndGet()
        }
    }

    suspend fun writeThrough(sku: String, request: UpdateProductRequest): ProductResponse {
        check(repository.accepts(sku)) { "Unknown product key is not admitted: $sku" }
        val name = request.name.trim()
        require(name.isNotBlank()) { "name must not be blank" }
        val updated = ProductResponse(
            sku = sku,
            name = name,
            version = repository.nextVersion(sku),
            source = "cache",
        )
        return try {
            repository.put(sku, updated)
            writeFailureLatch.set(false)
            updated.copy(source = "write-through")
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            repository.invalidate(sku)
            writeFailureLatch.set(true)
            throw cause
        }
    }

    suspend fun invalidate(sku: String): Boolean {
        if (!repository.accepts(sku)) return false
        val present = repository.cache.getIfPresent(sku) != null
        if (present) repository.invalidate(sku)
        return present
    }

    fun stats(): CoroutineCacheStatsResponse =
        CoroutineCacheStatsResponse(
            databaseReads = repository.databaseReads,
            cacheHits = cacheHits.get(),
            cacheMisses = cacheMisses.get(),
            inFlightLoads = inFlightLoads.get(),
            cacheSize = repository.cache.asMap().size,
        )

    fun writeFailureLatched(): Boolean = writeFailureLatch.get()
}
