package exposed.multitenant.webflux.tenant

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
class TenantInitializer(
    private val dataInitializer: DataInitializer,
): ApplicationListener<ApplicationReadyEvent> {

    companion object: KLoggingChannel()

    /**
     * 각 테넌트에 대한 스키마를 생성하고, 예제 데이터를 등록합니다.
     */
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        Tenants.Tenant.entries.forEach { tenant ->
            runBlocking(Dispatchers.IO) {
                dataInitializer.initialize(tenant)
            }
        }
    }
}
