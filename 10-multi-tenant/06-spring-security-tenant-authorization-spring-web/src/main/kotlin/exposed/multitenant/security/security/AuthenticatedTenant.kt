package exposed.multitenant.security.security

import exposed.multitenant.security.tenant.TenantId
import java.io.Serializable

/**
 * 인증된 Spring Security principal에서 추출한 테넌트 식별자이다.
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
