package exposed.multitenant.springweb.tenant

import io.bluetape4k.logging.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TenantService(
    private val dataInitializer: DataInitializer,
) {
    companion object: KLogging()

    @Transactional
    fun createTenantSchemas() {
        Tenants.Tenant.entries.forEach { tenant ->
            TenantContext.setCurrentTenant(tenant)
            dataInitializer.initialize()
        }
    }
}
