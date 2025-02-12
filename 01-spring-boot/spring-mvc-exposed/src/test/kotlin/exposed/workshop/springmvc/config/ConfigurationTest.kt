package exposed.workshop.springmvc.config

import exposed.workshop.springmvc.AbstractSpringMvcTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.junit.jupiter.api.Test

class ConfigurationTest: AbstractSpringMvcTest() {

    companion object: KLogging()

    @Test
    fun `context loading`() {
        log.info { "Context loading test" }
    }
}
