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
        TenantContext.withTenant(request.getHeader(TENANT_HEADER)) {
            filterChain.doFilter(request, response)
        }
    }

    companion object {
        /**
         * 현재 요청의 tenant 식별자를 전달하는 HTTP 헤더 이름입니다.
         */
        const val TENANT_HEADER = "X-Tenant-Id"
    }
}
