package exposed.examples.springwebflux.config

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.repository.ActorRepository
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ConfigurationTest: AbstractCoroutineExposedRepositoryTest() {

    @Autowired
    private val actorRepository: ActorRepository = uninitialized()

    @Test
    fun `context loading`() {
        actorRepository.shouldNotBeNull()
    }
}
