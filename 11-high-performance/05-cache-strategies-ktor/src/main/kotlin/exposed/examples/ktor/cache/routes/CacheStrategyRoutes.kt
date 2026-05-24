package exposed.examples.ktor.cache.routes

import exposed.examples.ktor.cache.model.UpsertUserRequest
import exposed.examples.ktor.cache.service.CachedUserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

internal fun Route.cacheStrategyRoutes(service: CachedUserService) {
    route("/users/{id}") {
        get("/cache-aside") {
            call.respond(service.cacheAside(call.requiredId()))
        }
        get("/read-through") {
            call.respond(service.readThrough(call.requiredId()))
        }
        put("/write-through") {
            call.respond(service.writeThrough(call.requiredId(), call.receive<UpsertUserRequest>()))
        }
        delete("/cache") {
            val removed = service.invalidate(call.requiredId())
            call.respond(if (removed) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
        }
    }
    get("/cache/stats") {
        call.respond(service.stats())
    }
}

private fun io.ktor.server.application.ApplicationCall.requiredId(): String =
    parameters["id"]?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("id path variable is required")
