package exposed.multitenant.springweb.tenant

import io.bluetape4k.logging.KLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
class TenantInitializer(
    private val dataInitializer: DataInitializer,
): ApplicationListener<ApplicationReadyEvent> {
    companion object: KLogging()

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        Tenants.Tenant.entries.forEach { tenant ->
            ScopedValue.where(TenantContext.CURRENT_TENANT, tenant).run {
                dataInitializer.initialize()
            }
        }
    }
}
