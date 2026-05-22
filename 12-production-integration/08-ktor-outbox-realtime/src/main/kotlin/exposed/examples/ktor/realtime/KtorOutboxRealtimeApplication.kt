package exposed.examples.ktor.realtime

import exposed.examples.ktor.realtime.config.installKtorPlugins
import exposed.examples.ktor.realtime.model.HealthResponse
import exposed.examples.ktor.realtime.model.IndexResponse
import exposed.examples.ktor.realtime.persistence.NotificationPersistence
import exposed.examples.ktor.realtime.repository.ExposedNotificationOutboxRepository
import exposed.examples.ktor.realtime.routes.notificationRoutes
import exposed.examples.ktor.realtime.service.NotificationService
import exposed.examples.ktor.realtime.service.RealtimeDelivery
import exposed.examples.ktor.realtime.service.RealtimeHub
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
        ktorOutboxRealtimeModule()
    }.start(wait = true)
}

internal fun Application.ktorOutboxRealtimeModule(
    persistence: NotificationPersistence = NotificationPersistence.inMemory(),
    realtimeDelivery: RealtimeDelivery? = null,
) {
    monitor.subscribe(ApplicationStopped) {
        persistence.close()
    }

    installKtorPlugins()

    val hub = RealtimeHub()
    val repository = ExposedNotificationOutboxRepository(persistence.database)
    val service = NotificationService(repository, realtimeDelivery ?: hub)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        notificationRoutes(service, hub)
    }
}
