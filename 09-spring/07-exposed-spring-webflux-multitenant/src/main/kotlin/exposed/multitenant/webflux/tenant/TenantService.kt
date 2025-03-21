package exposed.multitenant.webflux.tenant

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.stereotype.Service

@Service
class TenantService(
    private val dataInitializer: DataInitializer,
) {
    companion object: KLogging()

    suspend fun createTenantSchemas() {
        Tenants.Tenant.entries.forEach { tenant ->
            withContext(Dispatchers.IO + TenantId(tenant)) {
                newSuspendedTransaction {
                    dataInitializer.initialize(tenant)
                }
            }
        }
    }
}
