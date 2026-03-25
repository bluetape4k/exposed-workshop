package exposed.multitenant.webflux

import exposed.multitenant.webflux.tenant.TenantInitializer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(proxyBeanMethods = false)
/**
 * WebFlux + Coroutines 기반 멀티테넌트 예제 애플리케이션입니다.
 */
class ExposedMultitenantWebfluxApp {

    companion object: KLoggingChannel()

    @Autowired
    private val tenantService: TenantInitializer = uninitialized()

}

/**
 * 애플리케이션을 리액티브 모드로 실행합니다.
 */
fun main(vararg args: String) {
    runApplication<ExposedMultitenantWebfluxApp>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
