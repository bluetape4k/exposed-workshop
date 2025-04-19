package exposed.examples.springcache.config

import exposed.examples.springcache.AbstractSpringCacheApplicationTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager

class LettuceRedisCacheConfigTest(
    @Autowired private val cacheManager: CacheManager,
): AbstractSpringCacheApplicationTest() {

    companion object: KLogging()


    @Test
    fun `context loading`() {
        cacheManager.shouldNotBeNull()
    }

    @Test
    fun `countries cache 를 생성한다`() {
        val countryCache = cacheManager.getCache("countries")
        countryCache.shouldNotBeNull()
    }
}
