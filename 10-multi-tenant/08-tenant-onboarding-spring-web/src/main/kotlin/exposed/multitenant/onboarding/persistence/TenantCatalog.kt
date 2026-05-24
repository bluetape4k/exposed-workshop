package exposed.multitenant.onboarding.persistence

import org.jetbrains.exposed.v1.core.Table

object TenantCatalog: Table("tenant_catalog") {
    val tenantId = varchar("tenant_id", 40)
    val displayName = varchar("display_name", 120)
    val provisionedSchema = varchar("schema_name", 80)
    val status = varchar("status", 24)

    override val primaryKey = PrimaryKey(tenantId)
}
