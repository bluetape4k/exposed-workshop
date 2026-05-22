package exposed.multitenant.schema.tenant

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TenantFilter : OncePerRequestFilter() {

    companion object {
        const val TENANT_HEADER = "X-Tenant-ID"
        private const val MAX_TENANT_HEADER_LENGTH = 64
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val tenant = resolveTenant(request)
        if (tenant == null) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"error":"invalid_tenant"}""")
            return
        }

        try {
            TenantContext.set(tenant)
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun resolveTenant(request: HttpServletRequest): TenantId? {
        val values = request.getHeaders(TENANT_HEADER).toList()
        if (values.size != 1) {
            return null
        }

        val value = values.single().trim()
        if (value.isBlank() || value.length > MAX_TENANT_HEADER_LENGTH || value.contains(',')) {
            return null
        }

        return TenantId.fromHeader(value)
    }
}
