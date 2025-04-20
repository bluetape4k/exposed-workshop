package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.AbstractSpringSuspendedCacheApplicationTest
import exposed.examples.suspendedcache.domain.DataPopulator
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

abstract class AbstractCountrySuspendedRepositoryTest: AbstractSpringSuspendedCacheApplicationTest() {

    companion object: KLogging()

    abstract val countrySuspendedRepository: CountrySuspendedRepository

    @Test
    fun `모든 국가 정보를 로드합니다`() = runSuspendIO {
        countrySuspendedRepository.evictCacheAll()

        log.debug { "1. 모든 국가 정보를 로드합니다..." }
        val countries = DataPopulator.COUNTRY_CODES.map { code ->
            countrySuspendedRepository.findByCode(code)
        }
        countries.all { it != null }.shouldBeTrue()

        log.debug { "2. 모든 국가 정보를 로드합니다..." }
        val countries2 = DataPopulator.COUNTRY_CODES.map { code ->
            countrySuspendedRepository.findByCode(code)
        }

        countries2.all { it != null }.shouldBeTrue()
    }

    @Test
    fun `국가 정보를 Update 합니다`() = runSuspendIO {
        countrySuspendedRepository.evictCacheAll()

        log.debug { "1. 모든 국가 정보를 로드합니다..." }
        val countries = DataPopulator.COUNTRY_CODES.map { code ->
            countrySuspendedRepository.findByCode(code)
        }
        countries.all { it != null }.shouldBeTrue()

        log.debug { "2. 모든 국가 정보를 Update 합니다..." }
        countries.forEach {
            it?.let {
                countrySuspendedRepository.update(it.copy(name = "${it.name} - updated"))
            }
        }

        log.debug { "3. 모든 국가 정보를 로드합니다..." }
        val countries2 = DataPopulator.COUNTRY_CODES.map { code ->
            countrySuspendedRepository.findByCode(code)
        }

        countries2.all { it != null }.shouldBeTrue()
    }
}
