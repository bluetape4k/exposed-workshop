package exposed.examples.springcache

import io.bluetape4k.logging.KLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringCacheApplication {

    companion object: KLogging()
}

fun main(vararg args: String) {
    runApplication<SpringCacheApplication>(*args)
}
