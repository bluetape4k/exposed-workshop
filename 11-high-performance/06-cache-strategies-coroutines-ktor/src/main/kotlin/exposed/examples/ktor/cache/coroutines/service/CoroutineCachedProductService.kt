package exposed.examples.ktor.cache.coroutines.service

import exposed.examples.ktor.cache.coroutines.model.CoroutineCacheStatsResponse
import exposed.examples.ktor.cache.coroutines.model.ProductResponse
import exposed.examples.ktor.cache.coroutines.model.UpdateProductRequest
import exposed.examples.ktor.cache.coroutines.repository.SuspendingProductRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class CoroutineCachedProductService(
    private val repository: SuspendingProductRepository,
) {
    private val cache = ConcurrentHashMap<String, ProductResponse>()
    private val loadLocks = ConcurrentHashMap<String, Mutex>()
    private val inFlightLoads = AtomicInteger()
    private val cacheHits = AtomicInteger()
    private val cacheMisses = AtomicInteger()

    suspend fun readThrough(sku: String): ProductResponse {
        cache[sku]?.let {
            cacheHits.incrementAndGet()
            return it.copy(source = "read-through-cache")
        }

        val lock = loadLocks.computeIfAbsent(sku) { Mutex() }
        return lock.withLock {
            cache[sku]?.let {
                cacheHits.incrementAndGet()
                return@withLock it.copy(source = "read-through-cache")
            }

            cacheMisses.incrementAndGet()
            inFlightLoads.incrementAndGet()
            try {
                val product = repository.find(sku) ?: throw IllegalArgumentException("Unknown product: $sku")
                cache[sku] = product.copy(source = "cache")
                product.copy(source = "read-through-database")
            } finally {
                inFlightLoads.decrementAndGet()
                loadLocks.remove(sku, lock)
            }
        }
    }

    suspend fun writeThrough(sku: String, request: UpdateProductRequest): ProductResponse {
        val name = request.name.trim()
        require(name.isNotBlank()) { "name must not be blank" }
        val updated = repository.update(sku, name)
        cache[sku] = updated.copy(source = "cache")
        return updated.copy(source = "write-through")
    }

    fun invalidate(sku: String): Boolean = cache.remove(sku) != null

    fun stats(): CoroutineCacheStatsResponse =
        CoroutineCacheStatsResponse(
            databaseReads = repository.databaseReads,
            cacheHits = cacheHits.get(),
            cacheMisses = cacheMisses.get(),
            inFlightLoads = inFlightLoads.get(),
            cacheSize = cache.size,
        )
}
