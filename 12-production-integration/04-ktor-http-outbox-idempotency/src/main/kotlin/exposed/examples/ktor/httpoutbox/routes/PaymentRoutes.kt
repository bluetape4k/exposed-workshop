package exposed.examples.ktor.httpoutbox.routes

import exposed.examples.ktor.httpoutbox.config.requirePayloadSize
import exposed.examples.ktor.httpoutbox.model.CreatePaymentRequest
import exposed.examples.ktor.httpoutbox.model.PaymentResponse
import exposed.examples.ktor.httpoutbox.service.PaymentService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json

internal fun Route.paymentRoutes(
    service: PaymentService,
    json: Json = Json { ignoreUnknownKeys = true },
) {
    route("/payments") {
        post {
            val response = service.submit(call.receiveSized(json))
            call.respond(if (response.duplicate) HttpStatusCode.OK else HttpStatusCode.Created, response)
        }
        post("/{id}/retry") {
            call.respond(service.retry(call.paymentId()))
        }
        get("/{id}") {
            call.respond(service.find(call.paymentId()))
        }
        get {
            call.respond(service.findAll())
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.receiveSized(json: Json): CreatePaymentRequest {
    val contentLength = request.headers["Content-Length"]?.toLongOrNull()
    if (contentLength != null) {
        requirePayloadSize(contentLength)
        return receive()
    }

    val body = receiveText()
    requirePayloadSize(body.toByteArray().size.toLong())
    return json.decodeFromString(CreatePaymentRequest.serializer(), body)
}

private fun io.ktor.server.application.ApplicationCall.paymentId(): Long =
    parameters["id"]?.toLongOrNull()
        ?: throw exposed.examples.ktor.httpoutbox.model.PaymentValidationException("id must be a number")
