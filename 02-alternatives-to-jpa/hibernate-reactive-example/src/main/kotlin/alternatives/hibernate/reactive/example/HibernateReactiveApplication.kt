package alternatives.hibernate.reactive.example

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HibernateReactiveApplication {
    companion object: KLoggingChannel()
}

fun main(args: Array<String>) {
    runApplication<HibernateReactiveApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
