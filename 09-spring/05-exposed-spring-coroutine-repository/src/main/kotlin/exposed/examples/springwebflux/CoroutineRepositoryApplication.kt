package exposed.examples.springwebflux

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoroutineRepositoryApplication {

    companion object: KLogging()

}

fun main(vararg args: String) {
    runApplication<CoroutineRepositoryApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
