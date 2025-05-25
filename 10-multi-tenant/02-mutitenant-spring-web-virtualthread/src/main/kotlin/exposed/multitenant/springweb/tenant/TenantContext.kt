package exposed.multitenant.springweb.tenant

import exposed.multitenant.springweb.tenant.Tenants.Tenant
import org.jetbrains.exposed.v1.core.Schema

object TenantContext {

    val CURRENT_TENANT: ScopedValue<Tenant> = ScopedValue.newInstance()

    fun getCurrentTenant(): Tenants.Tenant = CURRENT_TENANT.get() ?: Tenants.DEFAULT_TENANT

    fun getCurrentTenantSchema(): Schema = getSchemaDefinition(getCurrentTenant())

    inline fun withTenant(
        tenant: Tenants.Tenant = getCurrentTenant(),
        crossinline block: () -> Unit,
    ) {
        ScopedValue.where(CURRENT_TENANT, tenant).run {
            block()
        }
    }
}
