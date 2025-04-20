package exposed.examples.suspendedcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SpringSuspendedCacheApplication {

    companion object: KLogging() {
        @JvmStatic
        val redisServer = RedisServer.Launcher.redis
    }

    @Bean
    fun redisClient(): RedisClient {
        return RedisClient.create(RedisURI.create(redisServer.url))
    }
}

fun main(vararg args: String) {
    runApplication<SpringSuspendedCacheApplication>(*args)
}
