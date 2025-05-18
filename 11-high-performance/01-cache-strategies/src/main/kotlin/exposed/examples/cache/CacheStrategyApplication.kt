package exposed.examples.cache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.RedisServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CacheStrategyApplication {

    companion object: KLoggingChannel() {
        @JvmStatic
        val redis = RedisServer.Launcher.redis
    }
}

fun main(args: Array<String>) {
    runApplication<CacheStrategyApplication>(*args)
}
