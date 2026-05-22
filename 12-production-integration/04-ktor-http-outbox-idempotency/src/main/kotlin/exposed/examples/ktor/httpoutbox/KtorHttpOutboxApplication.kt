package exposed.examples.ktor.httpoutbox

import exposed.examples.ktor.httpoutbox.client.KtorPaymentGateway
import exposed.examples.ktor.httpoutbox.client.PaymentGateway
import exposed.examples.ktor.httpoutbox.config.installKtorPlugins
import exposed.examples.ktor.httpoutbox.model.HealthResponse
import exposed.examples.ktor.httpoutbox.model.IndexResponse
import exposed.examples.ktor.httpoutbox.persistence.PaymentPersistence
import exposed.examples.ktor.httpoutbox.repository.ExposedPaymentOutboxRepository
import exposed.examples.ktor.httpoutbox.routes.paymentRoutes
import exposed.examples.ktor.httpoutbox.service.PaymentService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val DEFAULT_PORT = 8080
private const val DEFAULT_EXTERNAL_BASE_URL = "https://payments.example.invalid"

fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT) {
        ktorHttpOutboxModule()
    }.start(wait = true)
}

internal fun Application.ktorHttpOutboxModule(
    persistence: PaymentPersistence = PaymentPersistence.inMemory(),
    paymentGateway: PaymentGateway = KtorPaymentGateway(DEFAULT_EXTERNAL_BASE_URL),
) {
    monitor.subscribe(ApplicationStopped) {
        persistence.close()
        if (paymentGateway is AutoCloseable) {
            paymentGateway.close()
        }
    }

    installKtorPlugins()

    val repository = ExposedPaymentOutboxRepository(persistence.database)
    val service = PaymentService(repository, paymentGateway)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        paymentRoutes(service)
    }
}
