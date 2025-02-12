package exposed.workshop.springwebflux

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringWebfluxApplication {

    companion object: KLogging()

}

fun main(vararg args: String) {
    runApplication<SpringWebfluxApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
