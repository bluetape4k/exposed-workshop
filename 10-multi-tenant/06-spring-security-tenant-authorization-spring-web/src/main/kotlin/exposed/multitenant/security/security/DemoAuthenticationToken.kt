package exposed.multitenant.security.security

import exposed.multitenant.security.tenant.TenantId
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * Authentication token for fixed workshop API-key and demo-session credentials.
 */
internal class DemoAuthenticationToken(
    val tenantId: TenantId,
    val source: CredentialSource,
) : AbstractAuthenticationToken(listOf(SimpleGrantedAuthority("ROLE_TENANT_USER"))) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any = "[PROTECTED]"

    override fun getPrincipal(): Any =
        "${source.name.lowercase()}:${tenantId.headerValue}"
}
