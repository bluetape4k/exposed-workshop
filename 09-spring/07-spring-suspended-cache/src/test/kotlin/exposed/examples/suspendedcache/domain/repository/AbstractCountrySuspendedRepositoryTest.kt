package exposed.examples.suspendedcache.domain.repository

import exposed.examples.suspendedcache.AbstractSpringSuspendedCacheApplicationTest
import exposed.examples.suspendedcache.domain.DataPopulator
import io.bluetape4k.coroutines.flow.extensions.flowFromSuspend
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

abstract class AbstractCountrySuspendedRepositoryTest: AbstractSpringSuspendedCacheApplicationTest() {

    companion object: KLoggingChannel()

    abstract val countrySuspendedRepository: CountrySuspendedRepository

    @Test
    fun `모든 국가 정보를 로드합니다`() = runSuspendIO {
        countrySuspendedRepository.evictCacheAll()

        log.debug { "1. 모든 국가 정보를 로드합니다..." }
        val countries = DataPopulator.COUNTRY_CODES.asFlow()
            .flatMapMerge { code ->
                flowFromSuspend {
                    countrySuspendedRepository.findByCode(code)
                }
            }.toList()
        countries.all { it != null }.shouldBeTrue()

        log.debug { "2. 모든 국가 정보를 로드합니다..." }
        val countries2 = DataPopulator.COUNTRY_CODES.asFlow()
            .flatMapMerge { code ->
                flowFromSuspend {
                    countrySuspendedRepository.findByCode(code)
                }
            }.toList()

        countries2.all { it != null }.shouldBeTrue()
    }

    @Test
    fun `국가 정보를 Update 합니다`() = runSuspendIO {
        countrySuspendedRepository.evictCacheAll()

        log.debug { "1. 모든 국가 정보를 로드합니다..." }
        val countries = DataPopulator.COUNTRY_CODES.asFlow()
            .flatMapMerge { code ->
                flowFromSuspend { countrySuspendedRepository.findByCode(code) }
            }.toList()
        countries.all { it != null }.shouldBeTrue()

        log.debug { "2. 모든 국가 정보를 Update 합니다..." }
        countries.asFlow()
            .flatMapMerge { country ->
                flowFromSuspend {
                    country?.let { countrySuspendedRepository.update(it.copy(name = "${it.name} - updated")) }
                }
            }
            .collect()

        log.debug { "3. 모든 국가 정보를 로드합니다..." }
        val countries2 = DataPopulator.COUNTRY_CODES.asFlow()
            .flatMapMerge(concurrency = Runtimex.availableProcessors) { code ->
                flowFromSuspend {
                    countrySuspendedRepository.findByCode(code)
                }
            }.toList()

        countries2.all { it != null }.shouldBeTrue()
    }
}
