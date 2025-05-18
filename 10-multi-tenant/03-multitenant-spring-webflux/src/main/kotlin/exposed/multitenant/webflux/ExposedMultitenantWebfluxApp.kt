package exposed.multitenant.webflux

import exposed.multitenant.webflux.tenant.TenantInitializer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExposedMultitenantWebfluxApp {

    companion object: KLoggingChannel()

    @Autowired
    private val tenantService: TenantInitializer = uninitialized()

}

fun main(vararg args: String) {
    runApplication<ExposedMultitenantWebfluxApp>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
