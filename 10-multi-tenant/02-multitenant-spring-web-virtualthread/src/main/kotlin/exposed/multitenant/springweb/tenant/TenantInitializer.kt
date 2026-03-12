package exposed.multitenant.springweb.tenant

import io.bluetape4k.logging.KLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

/**
 * 애플리케이션 시작 시 테넌트별 스키마와 샘플 데이터를 초기화합니다.
 */
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
