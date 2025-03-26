package exposed.multitenant.springweb.tenant

import io.bluetape4k.logging.KLogging
import org.springframework.stereotype.Service

@Service
class TenantService(
    private val dataInitializer: DataInitializer,
) {
    companion object: KLogging()

    fun createTenantSchemas() {
        Tenants.Tenant.entries.forEach { tenant ->
            ScopedValue.where(TenantContext.CURRENT_TENANT, tenant).run {
                dataInitializer.initialize()
            }
        }
    }
}
