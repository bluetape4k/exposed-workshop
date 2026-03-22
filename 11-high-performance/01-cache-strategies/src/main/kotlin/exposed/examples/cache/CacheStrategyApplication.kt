package exposed.examples.cache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
/**
 * Spring MVC + Virtual Threads 기반 캐시 전략 데모를 실행하는 애플리케이션입니다.
 */
class CacheStrategyApplication {

    companion object: KLoggingChannel() 
}

fun main(args: Array<String>) {
    runApplication<CacheStrategyApplication>(*args)
}
