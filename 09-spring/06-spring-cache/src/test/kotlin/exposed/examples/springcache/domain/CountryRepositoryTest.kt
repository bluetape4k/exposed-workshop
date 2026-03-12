package exposed.examples.springcache.domain

import exposed.examples.springcache.AbstractSpringCacheApplicationTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

/** Redis 캐시를 활용한 `CountryRepository`의 조회 성능 향상, 캐시 evict, 존재하지 않는 코드 처리를 테스트합니다. */
class CountryRepositoryTest(
    @param:Autowired private val countryRepository: CountryRepository,
    @param:Autowired private val cacheManager: CacheManager,
): AbstractSpringCacheApplicationTest() {

    companion object: KLogging()

    /**
     * 첫 조회로 캐시를 채우고, 이후 조회는 같은 결과를 반환해야 한다.
     */
    @Test
    fun `get country at first`() {
        val countryCache: Cache = cacheManager.getCache(CountryRepository.COUNTRY_CACHE_NAME)!!
        countryRepository.evictCacheAll()
        DataPopulator.COUNTRY_CODES.all { countryCache.get("country:$it") == null }.shouldBeTrue()

        log.debug { "처음 조회 시에는 DB에서 읽어온다 ..." }
        val countriesFromDb = DataPopulator.COUNTRY_CODES.mapNotNull { code ->
            countryRepository.findByCode(code)
        }
        countriesFromDb.size shouldBeEqualTo DataPopulator.COUNTRY_CODES.size
        DataPopulator.COUNTRY_CODES.all { countryCache.get("country:$it") != null }.shouldBeTrue()

        log.debug { "두번째 조회 시에는 캐시에서 읽어온다..." }
        val countriesFromCache = DataPopulator.COUNTRY_CODES.mapNotNull { code ->
            countryRepository.findByCode(code)
        }
        countriesFromCache shouldBeEqualTo countriesFromDb
    }

    /**
     * Country를 업데이트 하면 기존 캐시가 비워지고, 다음 조회에서 최신 값으로 다시 채워져야 한다.
     */
    @Test
    fun `Country Update 할 때 cache evict를 수행한다`() {
        val countryCache: Cache = cacheManager.getCache(CountryRepository.COUNTRY_CACHE_NAME)!!
        countryRepository.evictCacheAll()
        DataPopulator.COUNTRY_CODES.all { countryCache.get("country:$it") == null }.shouldBeTrue()

        log.debug { "처음 Country를 조회합니다. (DB로부터 읽어온다)" }
        val countries = DataPopulator.COUNTRY_CODES.mapNotNull { code -> countryRepository.findByCode(code) }
        countries.size shouldBeEqualTo DataPopulator.COUNTRY_CODES.size
        DataPopulator.COUNTRY_CODES.all { countryCache.get("country:$it") != null }.shouldBeTrue()

        log.debug { "모든 Country 를 Update 합니다." }
        countries.forEach { country ->
            val updatedCountry = country.copy(description = "Updated description")
            countryRepository.update(updatedCountry)
        }
        DataPopulator.COUNTRY_CODES.all { countryCache.get("country:$it") == null }.shouldBeTrue()

        log.debug { "Update 된 Country 를 조회합니다. (DB로부터 읽어온다)" }
        val updatedCountries = DataPopulator.COUNTRY_CODES.mapNotNull { code -> countryRepository.findByCode(code) }
        updatedCountries.size shouldBeEqualTo countries.size
        updatedCountries.all { it.description == "Updated description" }.shouldBeTrue()

        log.debug { "모든 Country 를 조회합니다 (캐시로부터 읽어온다)" }
        val cachedCountries = DataPopulator.COUNTRY_CODES.mapNotNull { code -> countryRepository.findByCode(code) }
        cachedCountries.size shouldBeEqualTo countries.size
        cachedCountries shouldBeEqualTo updatedCountries
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

    @Test
    fun `존재하지 않는 국가 코드를 조회하면 null을 반환한다`() {
        countryRepository.findByCode("NO_SUCH_COUNTRY").shouldBeNull()
    }
}
