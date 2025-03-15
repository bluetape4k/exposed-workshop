package exposed.workshop.springmvc

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringMvcApplication {
    companion object: KLogging()
}

fun main(vararg args: String) {
    runApplication<SpringMvcApplication>(*args) {
        webApplicationType = WebApplicationType.SERVLET
    }
}
