package exposed.multitenant.security.security

import jakarta.servlet.http.HttpServletRequest

internal object DemoSecurityPaths {

    fun isHealthEndpoint(request: HttpServletRequest): Boolean {
        val servletPath = request.servletPath.takeIf(String::isNotBlank)
        val requestPath = servletPath ?: request.requestURI.removePrefix(request.contextPath)
        return requestPath == "/actuator/health" || requestPath.startsWith("/actuator/health/")
    }
}
