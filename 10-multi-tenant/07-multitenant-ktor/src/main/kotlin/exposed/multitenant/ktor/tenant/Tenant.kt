package exposed.multitenant.ktor.tenant

enum class Tenant(
    val id: String,
    val schema: String,
    val seedTitles: List<String>,
) {
    ACME(
        id = "acme",
        schema = "TENANT_ACME",
        seedTitles = listOf("ACME onboarding movie", "ACME isolation movie"),
    ),
    GLOBEX(
        id = "globex",
        schema = "TENANT_GLOBEX",
        seedTitles = listOf("Globex onboarding movie", "Globex isolation movie"),
    ),
    ;

    companion object {
        const val HEADER_NAME: String = "X-Tenant-ID"

        fun fromHeader(value: String?): Tenant? =
            value
                ?.trim()
                ?.lowercase()
                ?.let { tenantId -> entries.firstOrNull { it.id == tenantId } }
    }
}
