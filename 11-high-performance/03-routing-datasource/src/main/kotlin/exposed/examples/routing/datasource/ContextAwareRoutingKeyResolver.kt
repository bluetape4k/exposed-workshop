package exposed.examples.routing.datasource

import exposed.examples.routing.context.TenantContext
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * tenant 컨텍스트와 트랜잭션 read-only 상태를 조합해 라우팅 키를 계산합니다.
 *
 * 키 포맷은 `<tenant>:<rw|ro>` 입니다.
 */
class ContextAwareRoutingKeyResolver(
    private val defaultTenant: String = "default",
    private val tenantSupplier: () -> String? = { TenantContext.currentTenant() },
    private val readOnlySupplier: () -> Boolean = { TransactionSynchronizationManager.isCurrentTransactionReadOnly() },
): RoutingKeyResolver {

    override fun currentLookupKey(): String {
        val tenant = tenantSupplier()
            ?.takeIf { it.isNotBlank() }
            ?: defaultTenant

        val mode = if (readOnlySupplier()) "ro" else "rw"
        return "$tenant:$mode"
    }
}

