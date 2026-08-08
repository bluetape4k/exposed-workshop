package exposed.examples.ktor.cache.service

import exposed.examples.ktor.cache.model.CacheStatsResponse
import exposed.examples.ktor.cache.model.UpsertUserRequest
import exposed.examples.ktor.cache.model.UserResponse
import exposed.examples.ktor.cache.repository.ExposedUserRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class CachedUserService(
    private val repository: ExposedUserRepository,
    private val writeFailureLatch: AtomicBoolean = AtomicBoolean(false),
) {
    private val cacheHits = AtomicInteger()
    private val cacheMisses = AtomicInteger()

    fun cacheAside(id: String): UserResponse {
        repository.cache.getIfPresent(id)?.let {
            cacheHits.incrementAndGet()
            return it.copy(source = "cache-aside-cache")
        }

        cacheMisses.incrementAndGet()
        val user = repository.findByIdFromDb(id) ?: throw IllegalArgumentException("Unknown user: $id")
        repository.cache.put(id, user.copy(source = "cache"))
        return user.copy(source = "cache-aside-database")
    }

    fun readThrough(id: String): UserResponse {
        val cacheHit = repository.cache.getIfPresent(id) != null
        val user = repository.get(id) ?: throw IllegalArgumentException("Unknown user: $id")
        if (cacheHit) {
            cacheHits.incrementAndGet()
            return user.copy(source = "read-through-cache")
        }
        cacheMisses.incrementAndGet()
        return user.copy(source = "read-through-database")
    }

    fun writeThrough(id: String, request: UpsertUserRequest): UserResponse {
        val displayName = request.displayName.trim()
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        val saved = UserResponse(
            id = id,
            displayName = displayName,
            version = repository.nextVersion(id),
            source = "cache",
        )
        return try {
            repository.put(id, saved)
            writeFailureLatch.set(false)
            saved.copy(source = "write-through")
        } catch (cause: Throwable) {
            repository.invalidate(id)
            writeFailureLatch.set(true)
            throw cause
        }
    }

    fun invalidate(id: String): Boolean {
        val present = repository.cache.getIfPresent(id) != null
        if (present) repository.invalidate(id)
        return present
    }

    fun stats(): CacheStatsResponse =
        CacheStatsResponse(
            databaseReads = repository.databaseReads,
            cacheHits = cacheHits.get(),
            cacheMisses = cacheMisses.get(),
            cacheSize = repository.cache.asMap().size,
        )
}
