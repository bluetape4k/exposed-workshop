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
        module = Application::ktorCoroutineCacheModule,
    ).start(wait = true)
}

fun Application.ktorCoroutineCacheModule(
    dataSource: HikariDataSource = defaultDataSource(),
) {
    installKtorCoroutineCachePlugins()

    val lease = JdbcCacheDatabaseLease.acquire(dataSource)
    val writeFailureLatch = AtomicBoolean(false)
    var repository: SuspendingProductRepository? = null
    try {
        val allowedSkus = ProductPersistence(lease.database).initialize()
        val activeRepository = SuspendingProductRepository(allowedSkus, assertOwner = lease::assertOwned)
        repository = activeRepository
        val service = CoroutineCachedProductService(activeRepository, writeFailureLatch)

        environment.monitor.subscribe(ApplicationStopped) {
            lease.release(activeRepository::close)
        }

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
                    ExposedKtorCacheContributor.custom("products-cache") {
                        if (service.writeFailureLatched()) ExposedKtorCacheStatus.DOWN else ExposedKtorCacheStatus.UP
                    },
                ),
            ),
        )

        routing {
            get("/") {
                call.respond(IndexResponse())
            }
            // 기존 교육용 caller 계약을 유지합니다. library 상세 route는 /healthz/exposed 입니다.
            get("/health") {
                call.respond(HealthResponse(status = "UP"))
            }
            coroutineCacheRoutes(service)
        }
    } catch (cause: Throwable) {
        lease.release { repository?.close() }
        throw cause
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
