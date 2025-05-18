package exposed.multitenant.springweb

import io.bluetape4k.logging.KLogging
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
class ExposedMultitenantApplication {
    companion object: KLogging()
}

fun main(vararg args: String) {
    runApplication<ExposedMultitenantApplication>(*args) {
        webApplicationType = WebApplicationType.SERVLET
    }
}
