package exposed.examples.cache.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * WebFlux + Coroutines 환경에서 캐시 전략을 실습하는 애플리케이션입니다.
 */
@SpringBootApplication(proxyBeanMethods = false)
class CacheStrategyApplication {

    companion object: KLoggingChannel() {

    }
}

fun main(args: Array<String>) {
    runApplication<CacheStrategyApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
