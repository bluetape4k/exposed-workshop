package exposed.workshop.springwebflux

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring WebFlux + Exposed 통합 예제 애플리케이션.
 *
 * Reactive 기반 Spring WebFlux 서버를 Exposed ORM과 함께 사용하는 방법을 보여줍니다.
 */
@SpringBootApplication
class SpringWebfluxApplication {
    companion object: KLoggingChannel()
}

/**
 * 애플리케이션 진입점.
 *
 * Spring WebFlux (Reactive) 모드로 애플리케이션을 시작합니다.
 *
 * @param args 커맨드라인 인수
 */
fun main(vararg args: String) {
    runApplication<SpringWebfluxApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
