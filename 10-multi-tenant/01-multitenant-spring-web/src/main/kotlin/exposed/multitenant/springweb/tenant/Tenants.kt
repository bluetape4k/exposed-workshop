package exposed.multitenant.springweb.tenant

import org.jetbrains.exposed.v1.core.Schema

object Tenants {

    val DEFAULT_TENANT = Tenant.KOREAN

    enum class Tenant(val id: String) {
        KOREAN("korean"),
        ENGLISH("english");

        companion object {
            fun fromId(id: String): Tenant? = entries.find { it.id == id }
        }
    }

    fun getById(tenantId: String): Tenant =
        Tenant.fromId(tenantId) ?: error("No tenant found for id: $tenantId")

    private val tenantSchemas = mapOf(
        Tenant.KOREAN to getSchemaDefinition(Tenant.KOREAN),
        Tenant.ENGLISH to getSchemaDefinition(Tenant.ENGLISH),
    )

    fun getTenantSchema(tenant: Tenant): Schema =
        tenantSchemas[tenant] ?: error("No schema found for tenant: ${tenant.id}")
}
