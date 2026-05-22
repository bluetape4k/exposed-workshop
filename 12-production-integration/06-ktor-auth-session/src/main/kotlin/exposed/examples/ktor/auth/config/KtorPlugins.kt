package exposed.examples.ktor.auth.config

import exposed.examples.ktor.auth.model.AuthSessionCookie
import exposed.examples.ktor.auth.model.ErrorResponse
import exposed.examples.ktor.auth.model.PermissionDeniedException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

internal fun Application.installKtorPlugins() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    install(Sessions) {
        cookie<AuthSessionCookie>("auth_session") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.maxAgeInSeconds = 60 * 60
            cookie.extensions["SameSite"] = "Lax"
        }
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
        exception<PermissionDeniedException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", cause.message ?: "Permission denied"))
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", cause.message ?: "Resource was not found"))
        }
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) {
                throw cause
            }
            KtorAuthLogger.log.error(cause) { "Unexpected authentication example error" }
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("INTERNAL_ERROR", "Unexpected server error"))
        }
    }
}

private object KtorAuthLogger : KLogging()
