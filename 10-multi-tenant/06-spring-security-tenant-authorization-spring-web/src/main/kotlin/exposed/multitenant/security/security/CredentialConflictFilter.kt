package exposed.multitenant.security.security

import exposed.multitenant.security.controller.ErrorResponse
import exposed.multitenant.security.tenant.TenantRequest
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 지원되는 여러 인증 정보 소스가 섞인 요청을 거부한다.
 */
internal class CredentialConflictFilter : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        DemoSecurityPaths.isHealthEndpoint(request)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val credentialValueCounts = listOf(
            countBearerAuthorizationValues(request),
            countNonBlankHeaderValues(request, DemoApiKeyAuthenticationFilter.API_KEY_HEADER),
            countNonBlankHeaderValues(request, DemoSessionAuthenticationFilter.SESSION_HEADER),
        )
        val sources = credentialValueCounts.count { it > 0 }

        if (sources > 1 || credentialValueCounts.any { it > 1 }) {
            TenantRequest.writeError(
                response = response,
                status = HttpServletResponse.SC_BAD_REQUEST,
                error = ErrorResponse("CONFLICTING_CREDENTIALS", "Use exactly one credential source"),
            )
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun countBearerAuthorizationValues(request: HttpServletRequest): Int =
        request.getHeaders(HttpHeaders.AUTHORIZATION).toList()
            .count { value ->
                value.trimStart().startsWith("Bearer ", ignoreCase = true)
            }

    private fun countNonBlankHeaderValues(
        request: HttpServletRequest,
        name: String,
    ): Int =
        request.getHeaders(name).toList().count { it.isNotBlank() }
}
