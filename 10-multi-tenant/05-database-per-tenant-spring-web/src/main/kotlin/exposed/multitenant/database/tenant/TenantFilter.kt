package exposed.multitenant.database.tenant

import exposed.multitenant.database.controller.ErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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
        val tenant = resolveTenantOrWriteError(request, response) ?: return

        try {
            TenantContext.set(tenant)
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun resolveTenantOrWriteError(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): TenantId? {
        val values = request.getHeaders(TENANT_HEADER).toList()
        if (values.size != 1) {
            writeError(
                response = response,
                status = HttpServletResponse.SC_BAD_REQUEST,
                error = ErrorResponse("MISSING_TENANT", "$TENANT_HEADER header is required"),
            )
            return null
        }

        val value = values.single().trim()
        if (value.isBlank() || value.length > MAX_TENANT_HEADER_LENGTH || value.contains(',')) {
            writeError(
                response = response,
                status = HttpServletResponse.SC_BAD_REQUEST,
                error = ErrorResponse("MISSING_TENANT", "$TENANT_HEADER header is required"),
            )
            return null
        }

        val tenant = TenantId.fromHeaderOrNull(value)
        if (tenant == null) {
            writeError(
                response = response,
                status = HttpServletResponse.SC_NOT_FOUND,
                error = ErrorResponse("UNKNOWN_TENANT", "Unknown tenant"),
            )
        }
        return tenant
    }

    private fun writeError(
        response: HttpServletResponse,
        status: Int,
        error: ErrorResponse,
    ) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.writer.write("""{"code":${error.code.toJsonLiteral()},"message":${error.message.toJsonLiteral()}}""")
    }

    private fun String.toJsonLiteral(): String =
        buildString(length + 2) {
            append('"')
            this@toJsonLiteral.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (char.code < 0x20) {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
                }
            }
            append('"')
        }
}
