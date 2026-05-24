package exposed.examples.ktor.routingdatasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.examples.ktor.routingdatasource.config.installRoutingDataSourcePlugins
import exposed.examples.ktor.routingdatasource.model.HealthResponse
import exposed.examples.ktor.routingdatasource.model.IndexResponse
import exposed.examples.ktor.routingdatasource.persistence.InventoryPersistence
import exposed.examples.ktor.routingdatasource.repository.RoutingInventoryRepository
import exposed.examples.ktor.routingdatasource.routes.routingDataSourceRoutes
import exposed.examples.ktor.routingdatasource.routing.DataSourceRole
import exposed.examples.ktor.routingdatasource.routing.RoutingPlugin
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.concurrent.atomic.AtomicInteger

private const val DEFAULT_PORT = 8080
private val databaseSequence = AtomicInteger()

fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT, module = Application::ktorRoutingDataSourceModule).start(wait = true)
}

fun Application.ktorRoutingDataSourceModule(
    dataSources: Map<DataSourceRole, HikariDataSource> = defaultDataSources(),
) {
    installRoutingDataSourcePlugins()
    install(RoutingPlugin)

    val databases = dataSources.mapValues { (_, dataSource) -> Database.connect(dataSource) }
    databases.forEach { (role, database) -> InventoryPersistence(database).initialize(role) }
    environment.monitor.subscribe(ApplicationStopped) {
        dataSources.values.forEach(HikariDataSource::close)
    }

    val repository = RoutingInventoryRepository(databases)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        routingDataSourceRoutes(repository)
    }
}

private fun defaultDataSources(): Map<DataSourceRole, HikariDataSource> =
    databaseSequence.incrementAndGet().let { sequence ->
    DataSourceRole.entries.associateWith { role ->
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:ktor_routing_${sequence}_${role.name.lowercase()};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                poolName = "ktor-routing-${role.name.lowercase()}"
                maximumPoolSize = 3
                isAutoCommit = false
            },
        )
    }
    }
