package exposed.examples.ktor.cache.coroutines

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.examples.ktor.cache.coroutines.config.installKtorCoroutineCachePlugins
import exposed.examples.ktor.cache.coroutines.model.HealthResponse
import exposed.examples.ktor.cache.coroutines.model.IndexResponse
import exposed.examples.ktor.cache.coroutines.persistence.ProductPersistence
import exposed.examples.ktor.cache.coroutines.repository.SuspendingProductRepository
import exposed.examples.ktor.cache.coroutines.routes.coroutineCacheRoutes
import exposed.examples.ktor.cache.coroutines.service.CoroutineCachedProductService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.jdbc.Database

private const val DEFAULT_PORT = 8080

fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT, module = Application::ktorCoroutineCacheModule).start(wait = true)
}

fun Application.ktorCoroutineCacheModule(
    dataSource: HikariDataSource = defaultDataSource(),
) {
    installKtorCoroutineCachePlugins()

    val database = Database.connect(dataSource)
    ProductPersistence(database).initialize()
    environment.monitor.subscribe(ApplicationStopped) {
        dataSource.close()
    }

    val service = CoroutineCachedProductService(SuspendingProductRepository(database))

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        coroutineCacheRoutes(service)
    }
}

private fun defaultDataSource(): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:ktor_coroutine_cache;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
            driverClassName = "org.h2.Driver"
            username = "sa"
            password = ""
            maximumPoolSize = 5
            isAutoCommit = false
        },
    )
