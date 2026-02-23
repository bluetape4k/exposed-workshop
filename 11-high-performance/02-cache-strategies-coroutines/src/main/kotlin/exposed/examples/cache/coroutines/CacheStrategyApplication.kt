package exposed.examples.cache.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.RedisServer
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
/**
 * WebFlux + Coroutines 환경에서 캐시 전략을 실습하는 애플리케이션입니다.
 */
class CacheStrategyApplication {

    companion object: KLoggingChannel() {
        @JvmStatic
        val redis = RedisServer.Launcher.redis
    }
}

fun main(args: Array<String>) {
    runApplication<CacheStrategyApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
