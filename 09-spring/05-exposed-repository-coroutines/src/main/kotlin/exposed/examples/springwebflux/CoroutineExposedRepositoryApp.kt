package exposed.examples.springwebflux

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoroutineExposedRepositoryApp {
    companion object: KLoggingChannel()
}

fun main(vararg args: String) {
    runApplication<CoroutineExposedRepositoryApp>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
