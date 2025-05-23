package exposed.examples.springwebflux.config

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.repository.ActorExposedRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ConfigurationTest: AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel()

    @Autowired
    private val actorRepository: ActorExposedRepository = uninitialized()

    @Test
    fun `context loading`() {
        actorRepository.shouldNotBeNull()
    }
}
