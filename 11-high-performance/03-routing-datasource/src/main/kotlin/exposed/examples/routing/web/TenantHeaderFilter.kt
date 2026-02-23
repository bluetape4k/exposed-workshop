package exposed.examples.routing.web

import exposed.examples.routing.context.TenantContext
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * `X-Tenant-Id` 헤더 값을 [TenantContext]에 바인딩하는 요청 필터입니다.
 */
@Component
class TenantHeaderFilter: OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val tenantId = request.getHeader(TENANT_HEADER)
        if (tenantId.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        TenantContext.withTenant(tenantId) {
            filterChain.doFilter(request, response)
        }
    }

    companion object {
        const val TENANT_HEADER = "X-Tenant-Id"
    }
}

