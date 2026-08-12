package exposed.examples.springcache.config

import exposed.examples.springcache.AbstractSpringCacheApplicationTest
import exposed.examples.springcache.domain.CountryRepository
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.redis.serializer.RedisBinarySerializers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager

/** Lettuce Redis 캐시 매니저가 정상적으로 생성되고 캐시 저장/조회가 동작함을 검증합니다. */
class LettuceRedisCacheConfigTest(
    @param:Autowired private val cacheManager: CacheManager,
): AbstractSpringCacheApplicationTest() {

    companion object: KLogging()

    @Test
    fun `countries cache를 생성해서 캐시 작업을 수행한다`() {
        val countryCache = cacheManager.getCache(CountryRepository.COUNTRY_CACHE_NAME)
        countryCache.shouldNotBeNull()

        countryCache.put("KR", "South Korea")
        val country = countryCache.get("KR", String::class.java)
        country.shouldNotBeNull()
    }

    @Test
    fun `LZ4 FastFory serializer로 값을 round trip 한다`() {
        val value = "South Korea"
        val serializer = RedisBinarySerializers.LZ4FastFory

        serializer.deserialize(serializer.serialize(value)) shouldBeEqualTo value
    }
}
