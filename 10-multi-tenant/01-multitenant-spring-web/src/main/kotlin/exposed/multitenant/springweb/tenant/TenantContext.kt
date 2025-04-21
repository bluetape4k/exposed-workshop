package exposed.multitenant.springweb.tenant

import org.jetbrains.exposed.sql.Schema

object TenantContext {

    private val currentTenant = ThreadLocal<Tenants.Tenant?>()

    fun setCurrentTenant(tenant: Tenants.Tenant) = currentTenant.set(tenant)

    fun getCurrentTenant(): Tenants.Tenant = currentTenant.get() ?: Tenants.DEFAULT_TENANT

    fun getCurrentTenantSchema(): Schema = getSchemaDefinition(getCurrentTenant())

    fun clear() = currentTenant.remove()

    inline fun withTenant(tenant: Tenants.Tenant, block: () -> Unit) {
        setCurrentTenant(tenant)
        try {
            block()
        } finally {
            clear()
        }
    }
}
