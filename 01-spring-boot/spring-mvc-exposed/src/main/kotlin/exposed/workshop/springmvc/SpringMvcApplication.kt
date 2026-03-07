package exposed.workshop.springmvc

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring MVC + Exposed 통합 예제 애플리케이션.
 *
 * Spring Boot 기반의 서블릿(블로킹) 웹 애플리케이션으로,
 * Jetbrains Exposed DSL/DAO를 사용하여 데이터베이스에 접근하는 방법을 보여줍니다.
 *
 * @see ExposedDatabaseConfig
 */
@SpringBootApplication
class SpringMvcApplication {
    companion object: KLogging()
}

/**
 * Spring MVC 애플리케이션 진입점.
 *
 * [WebApplicationType.SERVLET] 타입으로 서블릿 기반 웹 서버를 시작합니다.
 *
 * @param args 커맨드라인 인수
 */
fun main(vararg args: String) {
    runApplication<SpringMvcApplication>(*args) {
        webApplicationType = WebApplicationType.SERVLET
    }
}
