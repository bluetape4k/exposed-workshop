package exposed.examples.ktor.architecture.config

import exposed.examples.ktor.architecture.model.CustomerValidationException
import exposed.examples.ktor.architecture.model.ErrorResponse
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import java.util.UUID
import kotlinx.coroutines.CancellationException

private object KtorArchitectureLogger : KLogging()

internal fun Application.installKtorPlugins() {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        callIdMdc("callId")
    }
    install(ContentNegotiation) {
        json(ApplicationJson)
    }
    install(StatusPages) {
        exception<PayloadTooLargeException> { call, _ ->
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                ErrorResponse(code = "PAYLOAD_TOO_LARGE", message = "Request body is too large")
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "BAD_REQUEST", message = "Malformed request body")
            )
        }
        exception<CustomerValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "VALIDATION_FAILED", message = cause.message ?: "Invalid request")
            )
        }
        exception<IllegalArgumentException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "VALIDATION_FAILED", message = "Invalid request")
            )
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(code = "NOT_FOUND", message = cause.message ?: "Resource not found")
            )
        }
        exception<Exception> { call, cause ->
            if (cause is CancellationException) {
                throw cause
            }
            KtorArchitectureLogger.log.error(cause) {
                "Unhandled failure in Ktor architecture example"
            }
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(code = "INTERNAL_ERROR", message = "Internal server error")
            )
        }
    }
}

internal class PayloadTooLargeException : RuntimeException()
