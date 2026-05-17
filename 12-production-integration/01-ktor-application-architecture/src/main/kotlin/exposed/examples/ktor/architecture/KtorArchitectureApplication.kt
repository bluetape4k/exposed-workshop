package exposed.examples.ktor.architecture

import exposed.examples.ktor.architecture.config.installKtorPlugins
import exposed.examples.ktor.architecture.model.HealthResponse
import exposed.examples.ktor.architecture.model.IndexResponse
import exposed.examples.ktor.architecture.persistence.CustomerPersistence
import exposed.examples.ktor.architecture.repository.ExposedCustomerRepository
import exposed.examples.ktor.architecture.routes.customerRoutes
import exposed.examples.ktor.architecture.service.CustomerService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val DEFAULT_PORT = 8080

/**
 * Starts the Ktor architecture example with an in-memory H2 database.
 *
 * ## Contract
 * - The route layer stays thin and delegates to a service.
 * - The repository owns every blocking Exposed JDBC transaction behind
 *   `Dispatchers.IO`.
 * - JSON errors are mapped to sanitized responses through `StatusPages`.
 */
fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT) {
        ktorArchitectureModule()
    }.start(wait = true)
}

/**
 * Configures the Ktor + Exposed architecture example.
 *
 * The default persistence uses an in-memory H2 database for local execution.
 * Tests pass a dedicated persistence instance with a unique JDBC URL per test.
 */
internal fun Application.ktorArchitectureModule(
    persistence: CustomerPersistence = CustomerPersistence.inMemory(),
) {
    monitor.subscribe(ApplicationStopped) {
        persistence.close()
    }

    installKtorPlugins()

    val repository = ExposedCustomerRepository(persistence.database)
    val service = CustomerService(repository)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        customerRoutes(service)
    }
}
