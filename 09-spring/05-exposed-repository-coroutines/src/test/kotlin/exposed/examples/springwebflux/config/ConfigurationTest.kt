package exposed.examples.springwebflux.config

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.repository.ActorExposedRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Spring WebFlux + Coroutines + Exposed Repository 모듈의 Spring 애플리케이션 컨텍스트 로딩을 검증합니다.
 */
class ConfigurationTest: AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel()

    @Autowired
    private val actorRepository: ActorExposedRepository = uninitialized()

    @Test
    fun `context loading`() {
        actorRepository.shouldNotBeNull()
    }
}
