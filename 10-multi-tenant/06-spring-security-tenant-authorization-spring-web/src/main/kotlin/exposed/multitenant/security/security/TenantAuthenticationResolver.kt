package exposed.multitenant.security.security

import exposed.multitenant.security.tenant.TenantId
import exposed.multitenant.security.tenant.TenantRequest
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * 인증된 Spring Security principal에서 테넌트 식별자를 추출한다.
 */
internal class TenantAuthenticationResolver {

    fun resolve(authentication: Authentication): AuthenticatedTenant {
        if (authentication is DemoAuthenticationToken) {
            return AuthenticatedTenant(authentication.tenantId, authentication.source)
        }

        if (authentication is JwtAuthenticationToken) {
            return resolveJwt(authentication)
        }

        throw TenantAuthorizationException("Missing tenant identity")
    }

    private fun resolveJwt(authentication: JwtAuthenticationToken): AuthenticatedTenant {
        val claim = authentication.token.claims[TENANT_CLAIM]
        val value = claim as? String ?: throw TenantAuthorizationException("Malformed tenant claim")
        if (value.isMalformedTenantValue()) {
            throw TenantAuthorizationException("Malformed tenant claim")
        }
        val tenantId = TenantId.fromHeaderOrNull(value)
            ?: throw TenantAuthorizationException("Unknown tenant claim")
        return AuthenticatedTenant(tenantId, CredentialSource.JWT)
    }

    private fun String.isMalformedTenantValue(): Boolean {
        val trimmed = trim()
        return trimmed.isBlank() ||
            trimmed.length > TenantRequest.MAX_TENANT_HEADER_LENGTH ||
            trimmed.contains(',') ||
            trimmed.any(Char::isWhitespace)
    }

    companion object {
        private const val TENANT_CLAIM = "tenant_id"
    }
}

internal class TenantAuthorizationException(message: String) : RuntimeException(message)
