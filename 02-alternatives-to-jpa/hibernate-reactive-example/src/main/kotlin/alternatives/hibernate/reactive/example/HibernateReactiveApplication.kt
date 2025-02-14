package alternatives.hibernate.reactive.example

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HibernateReactiveApplication {

    companion object: KLogging()

}

fun main(args: Array<String>) {
    runApplication<HibernateReactiveApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
