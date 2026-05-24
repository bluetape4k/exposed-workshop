package exposed.examples.ktor.cache.coroutines.routes

import exposed.examples.ktor.cache.coroutines.model.UpdateProductRequest
import exposed.examples.ktor.cache.coroutines.service.CoroutineCachedProductService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

internal fun Route.coroutineCacheRoutes(service: CoroutineCachedProductService) {
    route("/products/{sku}") {
        get("/read-through") {
            call.respond(service.readThrough(call.requiredSku()))
        }
        put("/write-through") {
            call.respond(service.writeThrough(call.requiredSku(), call.receive<UpdateProductRequest>()))
        }
        delete("/cache") {
            val removed = service.invalidate(call.requiredSku())
            call.respond(if (removed) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
        }
    }
    get("/cache/stats") {
        call.respond(service.stats())
    }
}

private fun io.ktor.server.application.ApplicationCall.requiredSku(): String =
    parameters["sku"]?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("sku path variable is required")
