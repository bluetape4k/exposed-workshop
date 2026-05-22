package exposed.multitenant.security.security

import exposed.multitenant.security.tenant.TenantContext
import exposed.multitenant.security.tenant.TenantRequest
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authorizes requested tenant access and binds the tenant context downstream.
 */
internal class TenantAuthorizationFilter(
    private val resolver: TenantAuthenticationResolver,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        DemoSecurityPaths.isHealthEndpoint(request)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw AccessDeniedException("Missing authentication")

        val authenticatedTenant = try {
            resolver.resolve(authentication)
        } catch (e: TenantAuthorizationException) {
            throw AccessDeniedException(e.message ?: "Tenant authorization failed", e)
        }

        val requestedTenant = TenantRequest.resolveTenantOrWriteError(request, response) ?: return
        if (authenticatedTenant.tenantId != requestedTenant) {
            throw AccessDeniedException("Tenant mismatch")
        }

        try {
            TenantContext.set(requestedTenant)
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
