package exposed.workshop.springwebflux

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringWebfluxApplication {
    companion object: KLoggingChannel()
}

fun main(vararg args: String) {
    runApplication<SpringWebfluxApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
