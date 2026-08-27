package exposed.examples.ktor.observability.config

import exposed.examples.ktor.observability.model.ErrorResponse
import io.bluetape4k.ktor.observability.Bluetape4kKtorObservabilityConfig
import io.bluetape4k.ktor.observability.CorrelationIdSettings
import io.bluetape4k.ktor.observability.installBluetape4kKtorObservability
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException

internal const val REQUEST_ID_HEADER = "X-Request-ID"
internal const val MAX_REQUEST_ID_LENGTH = 120

private object KtorObservabilityLogger : KLogging()

internal fun Application.installKtorPlugins() {
    installBluetape4kKtorObservability(
        Bluetape4kKtorObservabilityConfig(
            correlationId = CorrelationIdSettings(
                requestHeaderName = REQUEST_ID_HEADER,
                responseHeaderName = REQUEST_ID_HEADER,
                mdcKey = "callId",
                maxLength = MAX_REQUEST_ID_LENGTH,
                propagateResponseHeader = true,
            ),
        ),
    )
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
