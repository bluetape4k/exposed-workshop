package exposed.examples.springcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringCacheApplication {

    companion object: KLogging() {
        @JvmStatic
        val redisServer = RedisServer.Launcher.redis
    }
}

fun main(vararg args: String) {
    runApplication<SpringCacheApplication>(*args)
}
