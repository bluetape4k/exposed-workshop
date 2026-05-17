package exposed.examples.ktor.architecture.routes

import exposed.examples.ktor.architecture.config.ApplicationJson
import exposed.examples.ktor.architecture.config.PayloadTooLargeException
import exposed.examples.ktor.architecture.model.CreateCustomerRequest
import exposed.examples.ktor.architecture.model.CustomerValidationException
import exposed.examples.ktor.architecture.service.CustomerService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.header
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readAvailable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import java.io.ByteArrayOutputStream

private const val MAX_REQUEST_BODY_BYTES = 64 * 1024L
private const val REQUEST_BODY_BUFFER_BYTES = 8 * 1024

/**
 * Registers customer routes for the architecture example.
 */
internal fun Route.customerRoutes(service: CustomerService) {
    route("/customers") {
        get {
            call.respond(service.findAll())
        }
        post {
            val request = call.receiveLimited<CreateCustomerRequest>()
            call.respond(HttpStatusCode.Created, service.create(request))
        }
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: throw CustomerValidationException("id must be a number")
            call.respond(service.find(id))
        }
    }
}

private suspend inline fun <reified T : Any> ApplicationCall.receiveLimited(): T {
    val contentLength = request.header(HttpHeaders.ContentLength)?.toLongOrNull()
    if (contentLength != null && contentLength > MAX_REQUEST_BODY_BYTES) {
        throw PayloadTooLargeException()
    }
    val body = receiveLimitedBody()
    return try {
        ApplicationJson.decodeFromString(body)
    } catch (e: SerializationException) {
        throw BadRequestException("Malformed request body", e)
    }
}

private suspend fun ApplicationCall.receiveLimitedBody(): String {
    val channel = receiveChannel()
    val buffer = ByteArray(REQUEST_BODY_BUFFER_BYTES)
    val body = ByteArrayOutputStream(REQUEST_BODY_BUFFER_BYTES)
    var totalBytes = 0L

    while (true) {
        val readBytes = channel.readAvailable(buffer, 0, buffer.size)
        if (readBytes == -1) {
            break
        }
        if (readBytes == 0) {
            continue
        }

        totalBytes += readBytes
        if (totalBytes > MAX_REQUEST_BODY_BYTES) {
            throw PayloadTooLargeException()
        }
        body.write(buffer, 0, readBytes)
    }

    return String(body.toByteArray(), Charsets.UTF_8)
}
