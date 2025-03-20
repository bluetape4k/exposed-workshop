package exposed.multitenant.springweb.tenant

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class TenantInteceptor: HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        request.getHeader("X-TENANT-ID")?.let {
            TenantContext.setCurrentTenant(Tenants.Tenant.fromId(it) ?: Tenants.DEFAULT_TENANT)
        }
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        TenantContext.clear()
    }
}
