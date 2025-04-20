package exposed.examples.suspendedcache.config

import exposed.examples.suspendedcache.AbstractSpringSuspendedCacheApplicationTest
import exposed.examples.suspendedcache.domain.repository.CountrySuspendedRepository
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class SuspendedRepositoryConfigTest(
    @Autowired @Qualifier("defaultCountrySuspendedRepository") private val defaultCountrySuspendedRepository: CountrySuspendedRepository,
    @Autowired @Qualifier("cachedCountrySuspendedRepository") private val cachedCountrySuspendedRepository: CountrySuspendedRepository,
): AbstractSpringSuspendedCacheApplicationTest() {

    companion object: KLogging()

    @Test
    fun `defaultCountrySuspendedRepository가 생성되어야 한다`() = runSuspendIO {
        defaultCountrySuspendedRepository.shouldNotBeNull()

        defaultCountrySuspendedRepository.findByCode("KR").shouldNotBeNull()
    }

    @Test
    fun `cachedCountrySuspendedRepository가 생성되어야 한다`() = runSuspendIO {
        cachedCountrySuspendedRepository.shouldNotBeNull()

        cachedCountrySuspendedRepository.findByCode("KR").shouldNotBeNull()
    }
}
