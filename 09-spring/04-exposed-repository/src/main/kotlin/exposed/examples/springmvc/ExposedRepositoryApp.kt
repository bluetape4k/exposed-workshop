package exposed.examples.springmvc

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring MVC 기반의 Exposed 리포지토리 예제 애플리케이션.
 *
 * Exposed DAO/DSL을 Spring MVC와 통합하여 REST API를 제공하는 예제입니다.
 * SERVLET 웹 애플리케이션 타입으로 실행됩니다.
 */
@SpringBootApplication
class ExposedRepositoryApp {
    companion object: KLogging()
}

/**
 * 애플리케이션 진입점.
 *
 * @param args 커맨드라인 인자
 */
fun main(vararg args: String) {
    runApplication<ExposedRepositoryApp>(*args) {
        webApplicationType = WebApplicationType.SERVLET
    }
}
