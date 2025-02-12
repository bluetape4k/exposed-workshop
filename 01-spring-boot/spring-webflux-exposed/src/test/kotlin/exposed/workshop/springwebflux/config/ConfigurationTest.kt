package exposed.workshop.springwebflux.config

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import exposed.workshop.springwebflux.domain.repository.ActorRepository
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ConfigurationTest: AbstractSpringWebfluxTest() {

    @Autowired
    private val actorRepository: ActorRepository = uninitialized()

    @Test
    fun `context loading`() {
        actorRepository.shouldNotBeNull()
    }
}
