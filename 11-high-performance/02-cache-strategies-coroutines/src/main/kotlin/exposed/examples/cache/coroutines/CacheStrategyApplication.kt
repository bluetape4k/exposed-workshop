package exposed.examples.cache.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CacheStrategyApplication {

    companion object: KLogging() {
        @JvmStatic
        val redis = RedisServer.Launcher.redis
    }
}

fun main(args: Array<String>) {
    runApplication<CacheStrategyApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
