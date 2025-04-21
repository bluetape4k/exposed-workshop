package exposed.multitenant.springweb.tenant

import io.bluetape4k.logging.KLogging
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class TenantFilter: Filter {

    companion object: KLogging() {
        const val TENANT_HEADER = "X-TENANT-ID"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val tenant = extractTenant(request as HttpServletRequest)
        ScopedValue.where(TenantContext.CURRENT_TENANT, tenant).run {
            chain.doFilter(request, response)
        }
    }

    private fun extractTenant(request: HttpServletRequest): Tenants.Tenant {
        val tenantHeader = request.getHeader(TENANT_HEADER) ?: return Tenants.DEFAULT_TENANT
        return Tenants.getById(tenantHeader)
    }

}
