package exposed.multitenant.webflux.tenant

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.stereotype.Service

@Service
class TenantService(
    private val dataInitializer: DataInitializer,
) {
    companion object: KLogging()

    /**
     * 각 테넌트에 대한 스키마를 생성하고, 예제 데이터를 등록합니다.
     */
    suspend fun createTenantSchemas() {
        Tenants.Tenant.entries.forEach { tenant ->
            newSuspendedTransaction {
                dataInitializer.initialize(tenant)
            }
        }
    }
}
