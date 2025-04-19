package exposed.examples.springmvc.config

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.repository.ActorExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ConfigurationTest: AbstractExposedRepositoryTest() {

    companion object: KLogging()

    @Autowired
    private val actorRepository: ActorExposedRepository = uninitialized()

    @Test
    fun `context loading`() {
        log.info { "Context loading test" }
        actorRepository.shouldNotBeNull()
    }
}
