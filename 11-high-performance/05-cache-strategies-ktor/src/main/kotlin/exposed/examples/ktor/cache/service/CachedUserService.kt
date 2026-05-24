package exposed.examples.ktor.cache.service

import exposed.examples.ktor.cache.model.CacheStatsResponse
import exposed.examples.ktor.cache.model.UpsertUserRequest
import exposed.examples.ktor.cache.model.UserResponse
import exposed.examples.ktor.cache.repository.ExposedUserRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class CachedUserService(
    private val repository: ExposedUserRepository,
) {
    private val cache = ConcurrentHashMap<String, UserResponse>()
    private val cacheHits = AtomicInteger()
    private val cacheMisses = AtomicInteger()

    fun cacheAside(id: String): UserResponse =
        cache[id]
            ?.markCacheHit("cache-aside")
            ?: loadFromDatabase(id, "cache-aside")

    fun readThrough(id: String): UserResponse =
        cache[id]
            ?.markCacheHit("read-through")
            ?: loadFromDatabase(id, "read-through")

    fun writeThrough(id: String, request: UpsertUserRequest): UserResponse {
        val displayName = request.displayName.trim()
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        val saved = repository.upsert(id, displayName)
        cache[id] = saved.copy(source = "cache")
        return saved.copy(source = "write-through")
    }

    fun invalidate(id: String): Boolean = cache.remove(id) != null

    fun stats(): CacheStatsResponse =
        CacheStatsResponse(
            databaseReads = repository.databaseReads,
            cacheHits = cacheHits.get(),
            cacheMisses = cacheMisses.get(),
            cacheSize = cache.size,
        )

    private fun loadFromDatabase(id: String, strategy: String): UserResponse {
        cacheMisses.incrementAndGet()
        val user = repository.find(id) ?: throw IllegalArgumentException("Unknown user: $id")
        cache[id] = user.copy(source = "cache")
        return user.copy(source = "$strategy-database")
    }

    private fun UserResponse.markCacheHit(strategy: String): UserResponse {
        cacheHits.incrementAndGet()
        return copy(source = "$strategy-cache")
    }
}
