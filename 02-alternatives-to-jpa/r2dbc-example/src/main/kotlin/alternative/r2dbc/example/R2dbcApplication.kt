package alternative.r2dbc.example

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class R2dbcApplication {
    companion object: KLoggingChannel()
}

fun main(vararg args: String) {
    runApplication<R2dbcApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
