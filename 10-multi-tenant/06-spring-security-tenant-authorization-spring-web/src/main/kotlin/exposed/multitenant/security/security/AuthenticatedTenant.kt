package exposed.multitenant.security.security

import exposed.multitenant.security.tenant.TenantId
import java.io.Serializable

/**
 * Tenant identity derived from an authenticated Spring Security principal.
 */
internal data class AuthenticatedTenant(
    val tenantId: TenantId,
    val source: CredentialSource,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal enum class CredentialSource {
    JWT,
    API_KEY,
    DEMO_SESSION,
}
