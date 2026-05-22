package exposed.multitenant.security.security

import exposed.multitenant.security.tenant.TenantId
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authenticates fixed API keys for the tenant authorization workshop.
 */
internal class DemoApiKeyAuthenticationFilter : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        DemoSecurityPaths.isHealthEndpoint(request) ||
            request.getHeader(API_KEY_HEADER).isNullOrBlank()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val tenant = apiKeys[request.getHeader(API_KEY_HEADER)?.trim()]
        if (tenant == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = DemoAuthenticationToken(tenant, CredentialSource.API_KEY)
        SecurityContextHolder.setContext(context)
        filterChain.doFilter(request, response)
    }

    companion object {
        const val API_KEY_HEADER = "X-API-Key"

        private val apiKeys = mapOf(
            "demo-acme-key" to TenantId.ACME,
            "demo-globex-key" to TenantId.GLOBEX,
        )
    }
}
