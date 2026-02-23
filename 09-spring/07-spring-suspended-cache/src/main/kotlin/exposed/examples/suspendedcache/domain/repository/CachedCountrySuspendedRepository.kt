package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryRecord
import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCache
import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCacheManager
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * [CountrySuspendedRepository] 조회 결과를 Redis 기반 서스펜디드 캐시에 적재하는 데코레이터 구현입니다.
 */
class CachedCountrySuspendedRepository(
    private val delegate: CountrySuspendedRepository,
    private val cacheManager: LettuceSuspendedCacheManager,
): CountrySuspendedRepository {

    companion object: KLoggingChannel() {
        const val CACHE_NAME = "caches:country:code"
    }

    private val cache: LettuceSuspendedCache<String, CountryRecord> by lazy {
        cacheManager.getOrCreate(
            name = CACHE_NAME,
            ttlSeconds = 60,
        )
    }

    /**
     * 캐시 우선 조회 후, 미스 시 delegate 결과를 캐시에 저장해 반환합니다.
     */
    override suspend fun findByCode(code: String): CountryRecord? {
        return cache.get(code)
            ?: delegate.findByCode(code)?.apply { cache.put(code, this) }
    }

    /**
     * 업데이트 전에 해당 코드 캐시를 무효화한 뒤 원본 저장소를 갱신합니다.
     */
    override suspend fun update(countryRecord: CountryRecord): Int {
        cache.evict(countryRecord.code)
        return delegate.update(countryRecord)
    }

    /**
     * 국가 코드 캐시를 전체 비웁니다.
     */
    override suspend fun evictCacheAll() {
        cache.clear()
    }
}
