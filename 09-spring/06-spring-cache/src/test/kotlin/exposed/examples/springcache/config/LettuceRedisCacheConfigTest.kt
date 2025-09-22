package exposed.examples.springcache.config

import exposed.examples.springcache.AbstractSpringCacheApplicationTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager

class LettuceRedisCacheConfigTest(
    @param:Autowired private val cacheManager: CacheManager,
): AbstractSpringCacheApplicationTest() {

    companion object: KLogging()

    @Test
    fun `countries cache를 생성해서 캐시 작업을 수행한다`() {
        val countryCache = cacheManager.getCache("countries")
        countryCache.shouldNotBeNull()

        countryCache.put("KR", "South Korea")
        val country = countryCache.get("KR", String::class.java)
        country.shouldNotBeNull()
    }
}
