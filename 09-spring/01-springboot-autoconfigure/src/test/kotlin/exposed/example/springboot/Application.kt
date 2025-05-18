package exposed.example.springboot

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class Application {
    companion object: KLoggingChannel()
}

fun main(vararg args: String) {
    runApplication<Application>(*args)
}
