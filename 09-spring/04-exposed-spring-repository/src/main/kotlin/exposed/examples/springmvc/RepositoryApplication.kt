package exposed.examples.springmvc

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RepositoryApplication {

    companion object: KLogging()

}

fun main(vararg args: String) {
    runApplication<RepositoryApplication>(*args) {
        webApplicationType = WebApplicationType.SERVLET
    }
}
