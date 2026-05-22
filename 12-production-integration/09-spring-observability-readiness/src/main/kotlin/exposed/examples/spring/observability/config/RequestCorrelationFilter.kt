package exposed.examples.spring.observability.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

internal const val REQUEST_ID_HEADER = "X-Request-ID"
internal const val REQUEST_ID_ATTRIBUTE = "requestId"
internal const val MAX_REQUEST_ID_LENGTH = 120

private val RequestIdPattern = Regex("[A-Za-z0-9._:-]+")

internal fun sanitizeRequestId(value: String?): String? =
    value
        ?.trim()
        ?.takeIf { it.length in 1..MAX_REQUEST_ID_LENGTH && RequestIdPattern.matches(it) }

@Component
internal class RequestCorrelationFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = sanitizeRequestId(request.getHeader(REQUEST_ID_HEADER))
            ?: UUID.randomUUID().toString()

        MDC.put("requestId", requestId)
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove("requestId")
        }
    }
}
