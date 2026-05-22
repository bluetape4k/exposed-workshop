package exposed.examples.ktor.realtime.routes

import exposed.examples.ktor.realtime.model.CreateNotificationRequest
import exposed.examples.ktor.realtime.model.RealtimeEvent
import exposed.examples.ktor.realtime.service.NotificationService
import exposed.examples.ktor.realtime.service.RealtimeHub
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val eventJson = Json {
    ignoreUnknownKeys = true
}

internal fun Route.notificationRoutes(
    service: NotificationService,
    realtimeHub: RealtimeHub,
) {
    post("/notifications") {
        call.respond(service.create(call.receive<CreateNotificationRequest>()))
    }
    post("/outbox/publish") {
        call.respond(service.publishPending())
    }
    get("/outbox") {
        call.respond(service.outbox())
    }
    webSocket("/events") {
        val after = call.request.queryParameters["after"]?.toLongOrNull() ?: 0L
        val sentEventIds = mutableSetOf<Long>()
        service.replayAfter(after).forEach { event ->
            sentEventIds += event.id
            sendEvent(event)
        }

        val liveJob = launch {
            realtimeHub.live().collect { event ->
                if (event.id > after && sentEventIds.add(event.id)) {
                    sendEvent(event)
                }
            }
        }
        try {
            for (frame in incoming) {
                if (frame is Frame.Close) {
                    break
                }
            }
        } finally {
            liveJob.cancel("websocket session closed")
        }
    }
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.sendEvent(event: RealtimeEvent) {
    send(eventJson.encodeToString(event))
}
