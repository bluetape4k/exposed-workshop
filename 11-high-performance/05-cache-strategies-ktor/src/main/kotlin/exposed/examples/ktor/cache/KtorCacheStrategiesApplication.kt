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
import io.bluetape4k.exposed.ktor.Bluetape4kExposedKtorConfig
import io.bluetape4k.exposed.ktor.ExposedKtorCacheContributor
import io.bluetape4k.exposed.ktor.ExposedKtorCacheReadinessConfig
import io.bluetape4k.exposed.ktor.ExposedKtorCacheStatus
import io.bluetape4k.exposed.ktor.installBluetape4kExposedKtor
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers

private const val DEFAULT_PORT = 8080

fun main() {
    embeddedServer(
        CIO,
        host = "127.0.0.1",
        port = DEFAULT_PORT,
        module = Application::ktorCacheStrategiesModule,
    ).start(wait = true)
}

fun Application.ktorCacheStrategiesModule(
    dataSource: HikariDataSource = defaultDataSource(),
) {
    installKtorCachePlugins()

    val lease = JdbcCacheDatabaseLease.acquire(dataSource)
    val writeFailureLatch = AtomicBoolean(false)
    val repository = ExposedUserRepository(assertOwner = lease::assertOwned)
    try {
        UserPersistence(lease.database).initialize()
        val service = CachedUserService(repository, writeFailureLatch)

        installBluetape4kExposedKtor(
            Bluetape4kExposedKtorConfig(
                jdbcDatabase = lease.database,
                jdbcBlockingDispatcher = Dispatchers.IO,
                installStatusPages = false,
                installHealthRoutes = true,
                healthPath = "/healthz/exposed",
                readinessPath = "/ready",
            ),
            ExposedKtorCacheReadinessConfig(
                listOf(
                    ExposedKtorCacheContributor.jdbcRepository("users") { repository.validateConsistency() },
                    ExposedKtorCacheContributor.custom("users-write") {
                        if (writeFailureLatch.get()) ExposedKtorCacheStatus.DOWN else ExposedKtorCacheStatus.UP
                    },
                ),
            ),
        )

        environment.monitor.subscribe(ApplicationStopped) {
            lease.release(repository::close)
        }

        routing {
            get("/") {
                call.respond(IndexResponse())
            }
            // 기존 교육용 caller 계약을 유지합니다. library 상세 route는 /healthz/exposed 입니다.
            get("/health") {
                call.respond(HealthResponse(status = "UP"))
            }
            cacheStrategyRoutes(service)
        }
    } catch (cause: Throwable) {
        lease.release(repository::close)
        throw cause
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
