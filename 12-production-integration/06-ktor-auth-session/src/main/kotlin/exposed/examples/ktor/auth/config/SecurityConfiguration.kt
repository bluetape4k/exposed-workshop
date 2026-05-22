package exposed.examples.ktor.auth.config

import exposed.examples.ktor.auth.service.AuthPrincipal
import exposed.examples.ktor.auth.service.AuthService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic

internal const val AUTH_BASIC = "auth-basic"

internal fun Application.installAuth(service: AuthService) {
    install(Authentication) {
        basic(AUTH_BASIC) {
            realm = "ktor-auth-session"
            validate { credentials ->
                service.authenticate(credentials.name, credentials.password)
                    ?.let { AuthPrincipal(it.username, it.roles) }
            }
        }
    }
}
