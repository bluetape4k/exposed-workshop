package exposed.multitenant.security.security

import exposed.multitenant.security.tenant.TenantId
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 서버 세션을 만들지 않고 고정 demo-session 헤더를 인증한다.
 */
internal class DemoSessionAuthenticationFilter : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        DemoSecurityPaths.isHealthEndpoint(request) ||
            request.getHeader(SESSION_HEADER).isNullOrBlank()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val tenant = sessions[request.getHeader(SESSION_HEADER)?.trim()]
        if (tenant == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = DemoAuthenticationToken(tenant, CredentialSource.DEMO_SESSION)
        SecurityContextHolder.setContext(context)
        filterChain.doFilter(request, response)
    }

    companion object {
        const val SESSION_HEADER = "X-Demo-Session"

        private val sessions = mapOf(
            "acme-session" to TenantId.ACME,
            "globex-session" to TenantId.GLOBEX,
        )
    }
}
