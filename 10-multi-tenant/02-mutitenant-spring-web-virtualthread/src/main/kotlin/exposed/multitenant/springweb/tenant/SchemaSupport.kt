package exposed.multitenant.springweb.tenant

import org.jetbrains.exposed.v1.core.Schema

internal fun getSchemaDefinition(tenant: Tenants.Tenant): Schema =
    Schema(
        tenant.id,
        defaultTablespace = "USERS",
        temporaryTablespace = "TEMP ",
        quota = "20M",
        on = "USERS"
    )
