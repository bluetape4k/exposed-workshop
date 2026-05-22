package exposed.examples.ktor.observability.config

import exposed.examples.ktor.observability.model.ErrorResponse
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.response.respond
import java.util.UUID
import kotlinx.coroutines.CancellationException

internal const val REQUEST_ID_HEADER = "X-Request-ID"
internal const val MAX_REQUEST_ID_LENGTH = 120

private val RequestIdPattern = Regex("[A-Za-z0-9._:-]+")

internal fun sanitizeRequestId(value: String?): String? =
    value
        ?.trim()
        ?.takeIf(::isValidRequestId)

private fun isValidRequestId(value: String): Boolean =
    value.length in 1..MAX_REQUEST_ID_LENGTH && RequestIdPattern.matches(value)

private object KtorObservabilityLogger : KLogging()

internal fun Application.installKtorPlugins() {
    install(CallId) {
        retrieve { call -> sanitizeRequestId(call.request.header(REQUEST_ID_HEADER)) }
        generate { UUID.randomUUID().toString() }
        verify(::isValidRequestId)
        replyToHeader(REQUEST_ID_HEADER)
    }
    install(CallLogging) {
        callIdMdc("callId")
    }
    install(ContentNegotiation) {
        json(ApplicationJson)
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    code = "VALIDATION_FAILED",
                    message = cause.message ?: "Validation failed",
                    requestId = call.callId.orEmpty(),
                )
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    code = "BAD_REQUEST",
                    message = "Malformed request",
                    requestId = call.callId.orEmpty(),
                )
            )
        }
        exception<Exception> { call, cause ->
            if (cause is CancellationException) {
                throw cause
            }
            KtorObservabilityLogger.log.error(cause) {
                "Unhandled failure in Ktor observability example"
            }
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    code = "INTERNAL_ERROR",
                    message = "Internal server error",
                    requestId = call.callId.orEmpty(),
                )
            )
        }
    }
}
