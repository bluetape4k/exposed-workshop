package exposed.examples.routing.config

import exposed.examples.routing.context.TenantContext
import exposed.examples.routing.datasource.DataSourceRegistry
import exposed.examples.routing.domain.RoutingMarkerRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * 예제 실행 직후 각 tenant/rw-ro 데이터소스에 마커 데이터를 초기화합니다.
 */
@Component
class RoutingMarkerInitializer(
    private val dataSourceRegistry: DataSourceRegistry,
    private val markerRepository: RoutingMarkerRepository,
): ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val tenants = dataSourceRegistry.keys()
            .map { it.substringBefore(":") }
            .toSortedSet()

        tenants.forEach { tenant ->
            withRoutingHint(readOnly = false) {
                TenantContext.withTenant(tenant) {
                    markerRepository.resetAndInsert("$tenant-rw")
                }
            }
            withRoutingHint(readOnly = true) {
                TenantContext.withTenant(tenant) {
                    markerRepository.resetAndInsert("$tenant-ro")
                }
            }
        }
    }

    private inline fun <T> withRoutingHint(readOnly: Boolean, block: () -> T): T {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(readOnly)
        return try {
            block()
        } finally {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)
        }
    }
}

