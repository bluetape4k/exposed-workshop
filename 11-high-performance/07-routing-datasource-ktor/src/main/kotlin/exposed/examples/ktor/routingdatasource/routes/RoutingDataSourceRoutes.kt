package exposed.examples.ktor.routingdatasource.routes

import exposed.examples.ktor.routingdatasource.model.UpdateInventoryRequest
import exposed.examples.ktor.routingdatasource.repository.RoutingInventoryRepository
import exposed.examples.ktor.routingdatasource.routing.withRoutingContext
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

internal fun Route.routingDataSourceRoutes(repository: RoutingInventoryRepository) {
    route("/inventory/{sku}") {
        get {
            call.withRoutingContext {
                call.respond(repository.find(call.requiredSku()))
            }
        }
        put {
            call.withRoutingContext {
                val request = call.receive<UpdateInventoryRequest>()
                call.respond(repository.update(call.requiredSku(), request.quantity))
            }
        }
    }
    get("/routing/stats") {
        call.respond(repository.stats())
    }
}

private fun io.ktor.server.application.ApplicationCall.requiredSku(): String =
    parameters["sku"]?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("sku path variable is required")
