package exposed.multitenant.database.tenant

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component

@Component
class TenantTransaction(
    private val registry: TenantDatabaseRegistry,
) {

    fun <T> execute(
        tenantId: TenantId = TenantContext.current(),
        block: () -> T,
    ): T =
        transaction(registry.databaseFor(tenantId)) {
            block()
        }
}
