package exposed.examples.suspendedcache.config

import exposed.examples.suspendedcache.AbstractSpringSuspendedCacheApplicationTest
import exposed.examples.suspendedcache.domain.CountryRecord
import exposed.examples.suspendedcache.lettuce.LettuceSuspendedCacheManager
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/** `LettuceSuspendedCacheManager`가 정상 생성되고 suspend 방식으로 캐시 저장/조회가 동작함을 검증합니다. */
class LettuceSuspendedCacheConfigTest(
    @param:Autowired private val lettuceSuspendedCacheManager: LettuceSuspendedCacheManager,
): AbstractSpringSuspendedCacheApplicationTest() {

    companion object: KLoggingChannel()

    @Test
    fun `context loading`() {
        lettuceSuspendedCacheManager.shouldNotBeNull()
    }

    @Test
    fun `get cache`() = runSuspendIO {
        val cache = lettuceSuspendedCacheManager.getOrCreate<String, CountryRecord>("countries")
        cache.shouldNotBeNull()

        val countryKr = CountryRecord("KR", "South Korea", "동해물과 백두산이 마르고 닳도록")
        val countryUs = CountryRecord("US", "United States of America", "미국 국가는 몰라요")

        cache.put(countryKr.code, countryKr)
        cache.put(countryUs.code, countryUs)

        delay(10)

        cache.get(countryKr.code) shouldBeEqualTo countryKr
        cache.get(countryUs.code) shouldBeEqualTo countryUs
    }

    @Test
    fun `LZ4 FastFory codec로 값을 round trip 한다`() {
        val value = CountryRecord("KR", "South Korea", "동해물과 백두산이 마르고 닳도록")
        val codec = LettuceBinaryCodecs.lz4FastFory<CountryRecord>()

        val encoded = codec.encodeValue(value)
        val decoded = codec.decodeValue(encoded)

        decoded shouldBeEqualTo value
    }
}
