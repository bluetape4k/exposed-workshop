package exposed.multitenant.database.tenant

import io.bluetape4k.exposed.tenant.jdbc.TenantJdbcResourceRegistry
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component

@Component
class TenantTransaction(
    private val registry: TenantJdbcResourceRegistry<TenantId>,
) {

    fun <T> execute(
        tenantId: TenantId = TenantContext.current(),
        block: () -> T,
    ): T =
        transaction(registry.databaseFor(tenantId)) {
            block()
        }
}
