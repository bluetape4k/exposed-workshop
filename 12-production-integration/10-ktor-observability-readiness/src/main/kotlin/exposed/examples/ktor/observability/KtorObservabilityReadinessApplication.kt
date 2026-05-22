package exposed.examples.ktor.observability

import exposed.examples.ktor.observability.config.installKtorPlugins
import exposed.examples.ktor.observability.model.IndexResponse
import exposed.examples.ktor.observability.persistence.DiagnosticsPersistence
import exposed.examples.ktor.observability.repository.ExposedDiagnosticsRepository
import exposed.examples.ktor.observability.routes.diagnosticsRoutes
import exposed.examples.ktor.observability.routes.readinessRoutes
import exposed.examples.ktor.observability.service.DiagnosticsService
import exposed.examples.ktor.observability.service.ReadinessState
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val DEFAULT_PORT = 8081

/**
 * Starts the Ktor observability and readiness example with in-memory H2.
 *
 * ## Contract
 * - `/readyz` performs a database-backed readiness check.
 * - Request correlation uses `X-Request-ID` and structured error responses.
 * - Slow operation diagnostics are persisted through Exposed JDBC.
 */
fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT) {
        ktorObservabilityReadinessModule()
    }.start(wait = true)
}

internal fun Application.ktorObservabilityReadinessModule(
    persistence: DiagnosticsPersistence = DiagnosticsPersistence.inMemory(),
    readinessState: ReadinessState = ReadinessState(),
) {
    monitor.subscribe(ApplicationStopped) {
        persistence.close()
    }

    installKtorPlugins()

    val repository = ExposedDiagnosticsRepository(persistence.database)
    val service = DiagnosticsService(repository)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        readinessRoutes(repository, readinessState)
        diagnosticsRoutes(service)
    }
}
