package exposed.examples.springwebflux

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring WebFlux 기반의 코루틴 Exposed 리포지토리 예제 애플리케이션.
 *
 * Exposed DAO/DSL을 코루틴과 Spring WebFlux와 통합하여 비동기 REST API를 제공하는 예제입니다.
 * REACTIVE 웹 애플리케이션 타입으로 실행됩니다.
 */
@SpringBootApplication
class CoroutineExposedRepositoryApp {
    companion object: KLoggingChannel()
}

/**
 * 애플리케이션 진입점.
 *
 * @param args 커맨드라인 인자
 */
fun main(vararg args: String) {
    runApplication<CoroutineExposedRepositoryApp>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
