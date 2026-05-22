package exposed.multitenant.security.tenant

import exposed.multitenant.security.controller.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import java.nio.charset.StandardCharsets

internal object TenantRequest {

    const val TENANT_HEADER = "X-Tenant-ID"
    const val MAX_TENANT_HEADER_LENGTH = 64

    fun resolveTenantOrWriteError(
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
        if (value.isBlank() ||
            value.length > MAX_TENANT_HEADER_LENGTH ||
            value.contains(',') ||
            value.any(Char::isWhitespace)
        ) {
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

    fun writeError(
        response: HttpServletResponse,
        status: Int,
        error: ErrorResponse,
    ) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.writer.write("""{"code":${error.code.toJsonLiteral()},"message":${error.message.toJsonLiteral()}}""")
    }

    fun String.toJsonLiteral(): String =
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
