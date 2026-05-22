package exposed.examples.ktor.auth

import exposed.examples.ktor.auth.config.installAuth
import exposed.examples.ktor.auth.config.installKtorPlugins
import exposed.examples.ktor.auth.model.HealthResponse
import exposed.examples.ktor.auth.model.IndexResponse
import exposed.examples.ktor.auth.persistence.AuthPersistence
import exposed.examples.ktor.auth.repository.ExposedAuthRepository
import exposed.examples.ktor.auth.routes.authRoutes
import exposed.examples.ktor.auth.service.AuthService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val DEFAULT_PORT = 8080

fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT) {
        ktorAuthSessionModule()
    }.start(wait = true)
}

internal fun Application.ktorAuthSessionModule(
    persistence: AuthPersistence = AuthPersistence.inMemory(),
) {
    monitor.subscribe(ApplicationStopped) {
        persistence.close()
    }

    val repository = ExposedAuthRepository(persistence.database)
    val service = AuthService(repository)

    installKtorPlugins()
    installAuth(service)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        authRoutes(service)
    }
}
