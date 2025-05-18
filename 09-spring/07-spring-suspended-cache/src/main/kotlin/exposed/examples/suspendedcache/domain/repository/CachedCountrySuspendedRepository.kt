package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryDTO
import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCache
import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCacheManager
import io.bluetape4k.logging.coroutines.KLoggingChannel

class CachedCountrySuspendedRepository(
    private val delegate: CountrySuspendedRepository,
    private val cacheManager: LettuceSuspendedCacheManager,
): CountrySuspendedRepository {

    companion object: KLoggingChannel() {
        const val CACHE_NAME = "caches:country:code"
    }

    private val cache: LettuceSuspendedCache<String, CountryDTO> by lazy {
        cacheManager.getOrCreate(
            name = CACHE_NAME,
            ttlSeconds = 60,
        )
    }

    override suspend fun findByCode(code: String): CountryDTO? {
        return cache.get(code)
            ?: delegate.findByCode(code)?.apply { cache.put(code, this) }
    }

    override suspend fun update(countryDTO: CountryDTO): Int {
        cache.evict(countryDTO.code)
        return delegate.update(countryDTO)
    }

    override suspend fun evictCacheAll() {
        cache.clear()
    }
}
