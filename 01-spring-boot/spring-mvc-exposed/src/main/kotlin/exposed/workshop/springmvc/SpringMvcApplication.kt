package exposed.workshop.springmvc

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring MVC + Exposed 통합 예제 애플리케이션.
 *
 * Servlet 기반 Spring MVC 웹 서버를 Exposed ORM과 함께 사용하는 방법을 보여줍니다.
 */
@SpringBootApplication(proxyBeanMethods = false)
class SpringMvcApplication {
    companion object: KLogging()
}

/**
 * 애플리케이션 진입점.
 *
 * Spring MVC (Servlet) 모드로 애플리케이션을 시작합니다.
 *
 * @param args 커맨드라인 인수
 */
fun main(vararg args: String) {
    runApplication<SpringMvcApplication>(*args) {
        webApplicationType = WebApplicationType.SERVLET
    }
}
