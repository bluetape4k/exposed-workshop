package exposed.multitenant.springweb

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
/**
 * Virtual Thread 기반 Spring MVC 멀티테넌트 예제 애플리케이션입니다.
 */
class ExposedMultitenantApplication {
    companion object: KLogging()
}

/**
 * 애플리케이션을 서블릿 모드로 실행합니다.
 */
fun main(vararg args: String) {
    runApplication<ExposedMultitenantApplication>(*args) {
        webApplicationType = WebApplicationType.SERVLET
    }
}
