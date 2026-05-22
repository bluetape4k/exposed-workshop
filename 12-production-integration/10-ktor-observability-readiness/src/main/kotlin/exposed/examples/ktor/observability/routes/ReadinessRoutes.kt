package exposed.examples.ktor.observability.routes

import exposed.examples.ktor.observability.model.ReadinessResponse
import exposed.examples.ktor.observability.repository.DiagnosticsRepository
import exposed.examples.ktor.observability.service.ReadinessState
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

internal fun Route.readinessRoutes(
    repository: DiagnosticsRepository,
    readinessState: ReadinessState,
) {
    get("/readyz") {
        val databaseReady = readinessState.isDatabaseAvailable() && repository.ping()
        val response = ReadinessResponse(
            status = if (databaseReady) "UP" else "DOWN",
            database = if (databaseReady) "reachable" else "degraded",
            requestId = call.callId.orEmpty(),
        )
        call.respond(
            status = if (databaseReady) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
            message = response,
        )
    }
}
