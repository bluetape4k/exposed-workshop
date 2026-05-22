package exposed.examples.ktor.httpoutbox.config

import exposed.examples.ktor.httpoutbox.model.ErrorResponse
import exposed.examples.ktor.httpoutbox.model.PaymentValidationException
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
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

private const val MAX_REQUEST_BODY_BYTES = 64L * 1024L

internal fun Application.installKtorPlugins() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate(16, "0123456789abcdef")
        verify { it.length in 8..64 }
    }

    install(CallLogging) {
        level = Level.INFO
        callIdMdc("call-id")
    }

    install(StatusPages) {
        exception<PaymentValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("VALIDATION_ERROR", cause.message ?: "Request validation failed")
            )
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("NOT_FOUND", cause.message ?: "Resource was not found")
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Malformed request body"))
        }
        exception<ContentTransformationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Malformed request body"))
        }
        exception<PayloadTooLargeException> { call, _ ->
            call.respond(HttpStatusCode.PayloadTooLarge, ErrorResponse("PAYLOAD_TOO_LARGE", "Request body is too large"))
        }
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) {
                throw cause
            }
            KtorHttpOutboxLogger.log.error(cause) { "Unexpected payment outbox error" }
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("INTERNAL_ERROR", "Unexpected server error"))
        }
    }
}

internal fun requirePayloadSize(bytes: Long) {
    if (bytes > MAX_REQUEST_BODY_BYTES) {
        throw PayloadTooLargeException()
    }
}

internal class PayloadTooLargeException : RuntimeException("Request body is too large")

private object KtorHttpOutboxLogger : KLogging()
