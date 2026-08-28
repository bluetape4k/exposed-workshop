package exposed.multitenant.security.tenant

import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TenantTransaction(
    private val registry: TenantDatabaseRegistry,
) {

    fun <T> execute(
        tenantId: TenantId = TenantContexts.current(),
        block: () -> T,
    ): T =
        transaction(registry.databaseFor(tenantId)) {
            block()
        }
}
