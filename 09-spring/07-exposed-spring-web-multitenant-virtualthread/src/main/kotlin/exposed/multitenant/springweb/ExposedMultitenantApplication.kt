package exposed.multitenant.springweb

import exposed.multitenant.springweb.tenant.TenantService
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
class ExposedMultitenantApplication {

    companion object: KLogging()

    @Autowired
    private val tenantService: TenantService = uninitialized()

    @PostConstruct
    fun initialize() {
        tenantService.createTenantSchemas()
    }
}

fun main(vararg args: String) {
    runApplication<ExposedMultitenantApplication>(*args) {
        webApplicationType = WebApplicationType.SERVLET
    }
}
