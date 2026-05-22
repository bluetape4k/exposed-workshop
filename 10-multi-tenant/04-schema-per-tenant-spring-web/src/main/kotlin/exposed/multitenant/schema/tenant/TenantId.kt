package exposed.multitenant.schema.tenant

import org.jetbrains.exposed.v1.core.Schema

enum class TenantId(
    val headerValue: String,
    val schemaName: String,
) {
    ACME("acme", "TENANT_ACME"),
    GLOBEX("globex", "TENANT_GLOBEX");

    val schema: Schema = Schema(schemaName)

    companion object {
        private val schemaNamePattern = Regex("^[A-Z_][A-Z0-9_]{0,63}$")

        fun fromHeader(value: String): TenantId? =
            entries.singleOrNull { it.headerValue == value }

        fun validateSchemaNames() {
            entries.forEach { tenant ->
                require(schemaNamePattern.matches(tenant.schemaName)) {
                    "Invalid schema identifier: ${tenant.schemaName}"
                }
            }
        }
    }
}
