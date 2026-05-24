package exposed.multitenant.ktor

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.ktor.model.HealthResponse
import exposed.multitenant.ktor.model.IndexResponse
import exposed.multitenant.ktor.persistence.TenantBootstrap
import exposed.multitenant.ktor.repository.ExposedMovieRepository
import exposed.multitenant.ktor.routes.movieRoutes
import exposed.multitenant.ktor.tenant.TenantPlugin
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database

private const val DEFAULT_PORT = 8080

fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT, module = Application::tenantKtorModule).start(wait = true)
}

fun Application.tenantKtorModule(
    dataSource: HikariDataSource = defaultDataSource(),
) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }
    install(CallLogging)
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to cause.message.orEmpty()))
        }
    }
    install(TenantPlugin)

    val database = Database.connect(dataSource)
    TenantBootstrap(database).initialize()
    environment.monitor.subscribe(ApplicationStopped) {
        dataSource.close()
    }

    val repository = ExposedMovieRepository(database)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        movieRoutes(repository)
    }
}

private fun defaultDataSource(): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:multitenant_ktor;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
            driverClassName = "org.h2.Driver"
            username = "sa"
            password = ""
            maximumPoolSize = 5
            isAutoCommit = false
        },
    )
