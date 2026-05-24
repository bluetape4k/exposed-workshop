package exposed.multitenant.ktor.routes

import exposed.multitenant.ktor.model.CreateMovieRequest
import exposed.multitenant.ktor.model.TenantResponse
import exposed.multitenant.ktor.repository.ExposedMovieRepository
import exposed.multitenant.ktor.tenant.TenantContext
import exposed.multitenant.ktor.tenant.withTenantContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.movieRoutes(repository: ExposedMovieRepository) {
    route("/tenant") {
        get {
            call.withTenantContext {
                val tenant = TenantContext.currentTenant()
                call.respond(TenantResponse(tenant = tenant.id, schema = tenant.schema))
            }
        }
    }

    route("/movies") {
        get {
            call.withTenantContext {
                call.respond(repository.findAll())
            }
        }
        post {
            call.withTenantContext {
                val request = call.receive<CreateMovieRequest>()
                call.respond(HttpStatusCode.Created, repository.create(request))
            }
        }
    }
}
