package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryRecord
import exposed.examples.suspendedcache.domain.DataPopulator
import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCacheManager
import io.bluetape4k.coroutines.flow.extensions.flowFromSuspend
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class CachedCountrySuspendedRepositoryTest(
    @param:Autowired private val suspendedCacheManager: LettuceSuspendedCacheManager,
): AbstractCountrySuspendedRepositoryTest() {

    companion object: KLoggingChannel()

    @Autowired
    @Qualifier("cachedCountrySuspendedRepository")
    override val countrySuspendedRepository: CountrySuspendedRepository = uninitialized()

    @Test
    fun `모든 캐시를 삭제한다`() = runSuspendIO {
        val countryCache =
            suspendedCacheManager.getOrCreate<String, CountryRecord>(CachedCountrySuspendedRepository.CACHE_NAME)


        // 캐시를 채운다.
        DataPopulator.COUNTRY_CODES.asFlow()
            .flatMapMerge { code ->
                flowFromSuspend {
                    countrySuspendedRepository.findByCode(code)
                }
            }
            .collect()
        DataPopulator.COUNTRY_CODES.all { code -> countryCache.get(code) != null }.shouldBeTrue()

        // 캐시를 전부 비우고, 모두 NULL 임을 확인한다.
        countrySuspendedRepository.evictCacheAll()
        DataPopulator.COUNTRY_CODES.all { code -> countryCache.get(code) == null }.shouldBeTrue()
    }

    @Test
    fun `국가 정보를 수정하면 캐시가 무효화되고 다음 조회에 최신 값이 반영된다`() = runSuspendIO {
        val countryCache =
            suspendedCacheManager.getOrCreate<String, CountryRecord>(CachedCountrySuspendedRepository.CACHE_NAME)
        val code = DataPopulator.COUNTRY_CODES.first()

        val original = countrySuspendedRepository.findByCode(code)
        assertEquals(original, countryCache.get(code))

        val updated = original!!.copy(name = "${original.name} (updated)")
        countrySuspendedRepository.update(updated)

        assertNull(countryCache.get(code))
        assertEquals(updated, countrySuspendedRepository.findByCode(code))
        assertEquals(updated, countryCache.get(code))
    }
}
