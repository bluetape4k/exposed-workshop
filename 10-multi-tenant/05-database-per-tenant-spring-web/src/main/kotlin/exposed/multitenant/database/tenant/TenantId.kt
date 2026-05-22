package exposed.multitenant.database.tenant

enum class TenantId(
    val headerValue: String,
) {
    ACME("acme"),
    GLOBEX("globex"),
    ;

    companion object {
        fun fromHeader(value: String): TenantId =
            fromHeaderOrNull(value)
                ?: throw UnknownTenantException("Unknown tenant: $value")

        fun fromHeaderOrNull(value: String): TenantId? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.headerValue == normalized }
        }
    }
}

class UnknownTenantException(message: String) : RuntimeException(message)
