package exposed.multitenant.springweb.tenant

import exposed.multitenant.springweb.tenant.Tenants.Tenant
import org.jetbrains.exposed.sql.Schema

object TenantContext {

    val CURRENT_TENANT: ScopedValue<Tenant> = ScopedValue.newInstance()

    fun getCurrentTenant(): Tenants.Tenant = CURRENT_TENANT.get() ?: Tenants.DEFAULT_TENANT

    fun getCurrentTenantSchema(): Schema = getSchemaDefinition(getCurrentTenant())

}
