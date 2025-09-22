package exposed.examples.springcache.domain

import exposed.examples.springcache.AbstractSpringCacheApplicationTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import kotlin.system.measureTimeMillis

class CountryRepositoryTest(
    @param:Autowired private val countryRepository: CountryRepository,
    @param:Autowired private val cacheManager: CacheManager,
): AbstractSpringCacheApplicationTest() {

    companion object: KLogging()

    /**
     * Redis Cache를 사용하여 Country를 조회하는 테스트
     *
     * ```shell
     * # 테스트 실행 결과
     * databaseLoadingTime=749 msec
     * cacheLoadingTime=169 msec
     * ```
     */
    @Test
    fun `get country at first`() {
        countryRepository.evictCacheAll()

        log.debug { "처음 조회 시에는 DB에서 읽어온다 ..." }
        val databaseLoadingTime = measureTimeMillis {
            val countries = DataPopulator.COUNTRY_CODES.map { code ->
                countryRepository.findByCode(code)
            }
            countries.all { it != null }.shouldBeTrue()
        }

        log.debug { "두번째 조회 시에는 캐시에서 읽어온다..." }
        val cacheLoadingTime = measureTimeMillis {
            val countries = DataPopulator.COUNTRY_CODES.map { code ->
                countryRepository.findByCode(code)
            }
            countries.all { it != null }.shouldBeTrue()
        }

        log.debug { "databaseLoadingTime=$databaseLoadingTime msec" }
        log.debug { "cacheLoadingTime=$cacheLoadingTime msec" }

        cacheLoadingTime shouldBeLessThan databaseLoadingTime
    }

    /**
     * Country를 업데이트 할 때 캐시를 evict하는 테스트
     *
     * ```shell
     * databaseLoadingTime=767 msec
     * reloadLoadingTime=384 msec
     * cacheLoadingTime=120 msec
     * ```
     */
    @Test
    fun `Country Update 할 때 cache evict를 수행한다`() {
        countryRepository.evictCacheAll()

        val countries: List<CountryDTO>

        log.debug { "처음 Country를 조회합니다. (DB로부터 읽어온다)" }
        val databaseLoadingTime = measureTimeMillis {
            countries = DataPopulator.COUNTRY_CODES.mapNotNull { code -> countryRepository.findByCode(code) }
        }

        log.debug { "모든 Country 를 Update 합니다." }
        countries.forEach { country ->
            val updatedCountry = country.copy(description = "Updated description")
            countryRepository.update(updatedCountry)
        }

        log.debug { "Update 된 Country 를 조회합니다. (DB로부터 읽어온다)" }
        val updatedCountries: List<CountryDTO>
        val reloadLoadingTime = measureTimeMillis {
            updatedCountries = DataPopulator.COUNTRY_CODES.mapNotNull { code -> countryRepository.findByCode(code) }
        }

        updatedCountries.size shouldBeEqualTo countries.size

        log.debug { "databaseLoadingTime=$databaseLoadingTime msec" }
        log.debug { "reloadLoadingTime=$reloadLoadingTime msec" }

        log.debug { "모든 Country 를 조회합니다 (캐시로부터 읽어온다)" }
        val cachedCountries: List<CountryDTO>
        val cacheLoadingTime = measureTimeMillis {
            cachedCountries = DataPopulator.COUNTRY_CODES.mapNotNull { code -> countryRepository.findByCode(code) }
        }
        cachedCountries.size shouldBeEqualTo countries.size
        log.debug { "cacheLoadingTime=$cacheLoadingTime msec" }
    }

    @Test
    fun `모든 캐시를 삭제한다`() {
        val countryCache: Cache = cacheManager.getCache(CountryRepository.COUNTRY_CACHE_NAME)!!
        countryRepository.evictCacheAll()

        // 캐시를 채운다.
        DataPopulator.COUNTRY_CODES.mapNotNull { code -> countryRepository.findByCode(code) }
        DataPopulator.COUNTRY_CODES.all { countryCache.get("country:$it") != null }.shouldBeTrue()

        // 캐시를 전부 비우고, 모두 NULL 임을 확인한다.
        countryRepository.evictCacheAll()
        DataPopulator.COUNTRY_CODES.all { countryCache.get("country:$it") == null }.shouldBeTrue()
    }
}
