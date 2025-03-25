package exposed.multitenant.webflux

import exposed.multitenant.webflux.tenant.TenantService
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.uninitialized
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExposedMultitenantWebfluxApp {

    companion object: KLogging()

    @Autowired
    private val tenantService: TenantService = uninitialized()

    /**
     * Webflux 에서는 `@Transactional` 을 사용하지 않으므로, injection 해줘야 connection을 맺는다.
     */
    @Autowired
    private val database: Database = uninitialized()

    @PostConstruct
    fun initialize() {
        runBlocking(Dispatchers.IO) {
            tenantService.createTenantSchemas()
        }
    }

}

fun main(vararg args: String) {
    runApplication<ExposedMultitenantWebfluxApp>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
