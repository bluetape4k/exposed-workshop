package exposed.examples.ktor.auth.routes

import exposed.examples.ktor.auth.config.AUTH_BASIC
import exposed.examples.ktor.auth.model.AuthSessionCookie
import exposed.examples.ktor.auth.model.ErrorResponse
import exposed.examples.ktor.auth.model.SessionsResponse
import exposed.examples.ktor.auth.model.toProfileResponse
import exposed.examples.ktor.auth.model.toResponse
import exposed.examples.ktor.auth.service.AuthPrincipal
import exposed.examples.ktor.auth.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

internal fun Route.authRoutes(service: AuthService) {
    route("/api") {
        get("/public") {
            call.respond(mapOf("status" to "anonymous access allowed"))
        }
        get("/session-profile") {
            val sessionCookie = call.sessions.get<AuthSessionCookie>()
            if (sessionCookie == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "auth_session cookie is required"))
                return@get
            }
            val profile = service.profileBySessionToken(sessionCookie.token)
            if (profile == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "auth_session cookie is invalid or expired"))
                return@get
            }
            call.respond(profile.toProfileResponse())
        }
        authenticate(AUTH_BASIC) {
            get("/profile") {
                call.respond(service.profile(call.authPrincipal().username).toProfileResponse())
            }
            get("/admin") {
                call.respond(service.adminProfile(call.authPrincipal().username).toProfileResponse())
            }
            post("/sessions") {
                val session = service.createSession(call.authPrincipal().username)
                val rawToken = checkNotNull(session.token) {
                    "Created session must include the raw token for cookie transport"
                }
                call.sessions.set(AuthSessionCookie(rawToken))
                call.respond(session.toResponse())
            }
            get("/sessions") {
                call.respond(
                    SessionsResponse(
                        service.sessions(call.authPrincipal().username).map { it.toResponse() }
                    )
                )
            }
        }
    }
}

private fun ApplicationCall.authPrincipal(): AuthPrincipal =
    checkNotNull(principal<AuthPrincipal>()) {
        "Authenticated principal was not installed"
    }
