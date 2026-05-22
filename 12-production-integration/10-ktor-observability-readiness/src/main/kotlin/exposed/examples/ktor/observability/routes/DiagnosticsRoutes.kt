package exposed.examples.ktor.observability.routes

import exposed.examples.ktor.observability.model.OperationsResponse
import exposed.examples.ktor.observability.model.toResponse
import exposed.examples.ktor.observability.service.DiagnosticsService
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

internal fun Route.diagnosticsRoutes(service: DiagnosticsService) {
    route("/diagnostics/operations") {
        get {
            call.respond(
                OperationsResponse(
                    operations = service.findOperations().map { it.toResponse() }
                )
            )
        }
        get("/{name}") {
            val name = call.parameters["name"].orEmpty()
            val delayMs = call.request.queryParameters["delayMs"]?.toLongOrNull() ?: 0L
            call.respond(
                service.runOperation(
                    name = name,
                    delayMs = delayMs,
                    requestId = call.callId.orEmpty(),
                ).toResponse()
            )
        }
    }
}
