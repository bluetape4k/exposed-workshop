package exposed.multitenant.springweb.tenant

import io.bluetape4k.logging.KLogging
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

/**
 * 요청 헤더(`X-TENANT-ID`)를 읽어 공통 `ScopedValueTenantContext`에 테넌트를 binding하는
 * 서블릿 필터입니다.
 */
@Component
class TenantFilter: Filter {

    companion object: KLogging() {
        const val TENANT_HEADER = "X-TENANT-ID"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val tenant = extractTenant(request as HttpServletRequest)
        TenantContexts.withTenant(tenant) {
            chain.doFilter(request, response)
        }
    }

    private fun extractTenant(request: HttpServletRequest): Tenants.Tenant {
        val tenantHeader = request.getHeader(TENANT_HEADER) ?: return Tenants.DEFAULT_TENANT
        return Tenants.getById(tenantHeader)
    }
}
