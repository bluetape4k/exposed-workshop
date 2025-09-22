package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.domain.CountryDTO
import exposed.examples.suspendedcache.domain.DataPopulator
import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCacheManager
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldBeTrue
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
            suspendedCacheManager.getOrCreate<String, CountryDTO>(CachedCountrySuspendedRepository.CACHE_NAME)


        // 캐시를 채운다.
        DataPopulator.COUNTRY_CODES.map { code -> countrySuspendedRepository.findByCode(code) }
        DataPopulator.COUNTRY_CODES.all { code -> countryCache.get(code) != null }.shouldBeTrue()

        // 캐시를 전부 비우고, 모두 NULL 임을 확인한다.
        countrySuspendedRepository.evictCacheAll()
        DataPopulator.COUNTRY_CODES.all { code -> countryCache.get(code) == null }.shouldBeTrue()
    }

}
