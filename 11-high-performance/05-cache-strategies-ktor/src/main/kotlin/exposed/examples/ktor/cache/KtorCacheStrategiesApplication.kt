package exposed.examples.ktor.cache

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.examples.ktor.cache.config.installKtorCachePlugins
import exposed.examples.ktor.cache.model.HealthResponse
import exposed.examples.ktor.cache.model.IndexResponse
import exposed.examples.ktor.cache.persistence.UserPersistence
import exposed.examples.ktor.cache.repository.ExposedUserRepository
import exposed.examples.ktor.cache.routes.cacheStrategyRoutes
import exposed.examples.ktor.cache.service.CachedUserService
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
    embeddedServer(CIO, port = DEFAULT_PORT, module = Application::ktorCacheStrategiesModule).start(wait = true)
}

fun Application.ktorCacheStrategiesModule(
    dataSource: HikariDataSource = defaultDataSource(),
) {
    installKtorCachePlugins()

    val database = Database.connect(dataSource)
    UserPersistence(database).initialize()
    environment.monitor.subscribe(ApplicationStopped) {
        dataSource.close()
    }

    val service = CachedUserService(ExposedUserRepository(database))

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        cacheStrategyRoutes(service)
    }
}

private fun defaultDataSource(): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:ktor_cache_strategies;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
            driverClassName = "org.h2.Driver"
            username = "sa"
            password = ""
            maximumPoolSize = 5
            isAutoCommit = false
        },
    )
