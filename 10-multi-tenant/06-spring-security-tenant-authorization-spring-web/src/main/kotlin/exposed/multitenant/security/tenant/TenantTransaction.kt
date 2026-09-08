package exposed.multitenant.security.tenant

import io.bluetape4k.exposed.tenant.jdbc.TenantJdbcResourceRegistry
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TenantTransaction(
    private val registry: TenantJdbcResourceRegistry<TenantId>,
) {

    fun <T> execute(
        tenantId: TenantId = TenantContexts.current(),
        block: () -> T,
    ): T =
        transaction(registry.databaseFor(tenantId)) {
            block()
        }
}
