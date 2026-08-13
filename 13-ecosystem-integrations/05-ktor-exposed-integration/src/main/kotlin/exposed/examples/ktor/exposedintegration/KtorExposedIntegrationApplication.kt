package exposed.examples.ktor.exposedintegration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.exposed.ktor.Bluetape4kExposedKtorConfig
import io.bluetape4k.exposed.ktor.bluetape4kExposedErrors
import io.bluetape4k.exposed.ktor.exposedJdbcTransaction
import io.bluetape4k.exposed.ktor.installBluetape4kExposedKtor
import io.bluetape4k.ktor.core.Bluetape4kKtorCoreConfig
import io.bluetape4k.ktor.core.bluetape4kErrorResponses
import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.bluetape4k.r2dbc.pool.connectionFactoryOptionsOf
import io.bluetape4k.r2dbc.pool.connectionPoolOf
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.r2dbc.pool.ConnectionPool
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import java.io.Serializable as JavaSerializable
import java.sql.SQLException
import java.time.Duration
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

internal object WorkshopNotes: LongIdTable("ktor_exposed_notes") {
    val title = varchar("title", 120)
    val body = text("body")
}

@Serializable
internal data class NoteRequest(
    val title: String,
    val body: String,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class NoteResponse(
    val id: Long,
    val title: String,
    val body: String,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal class KtorExposedIntegrationResources private constructor(
    private val dataSource: HikariDataSource,
    internal val r2dbcPool: ConnectionPool,
    val jdbcDatabase: Database,
    val r2dbcDatabase: R2dbcDatabase,
    val jdbcDispatcher: ExecutorCoroutineDispatcher,
): AutoCloseable {

    fun closeJdbcDataSourceOnly() {
        runCatching { dataSource.close() }
    }

    override fun close() {
        runCatching { r2dbcPool.disposeLater().block(Duration.ofSeconds(5)) }
        runCatching { dataSource.close() }
        runCatching { jdbcDispatcher.close() }
    }

    companion object {
        fun create(name: String = "default"): KtorExposedIntegrationResources {
            val databaseName = "ktor-exposed-integration-$name"
            val dataSource = HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:$databaseName-jdbc;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL"
                    driverClassName = "org.h2.Driver"
                    username = "sa"
                    maximumPoolSize = 2
                    poolName = "$databaseName-jdbc"
                }
            )
            val jdbcDatabase = Database.connect(dataSource)
            val jdbcDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

            val r2dbcUrl = "r2dbc:h2:mem:///$databaseName-r2dbc;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
            val r2dbcOptions = connectionFactoryOptionsOf(r2dbcUrl)
            val r2dbcPool = connectionPoolOf(r2dbcOptions) {
                maxSize = 2
                initialSize = 1
                minIdle = 0
            }
            val r2dbcDatabase = R2dbcDatabase.connect(
                r2dbcPool,
                databaseConfig = R2dbcDatabaseConfig {
                    connectionFactoryOptions = r2dbcOptions
                },
            )

            val resources = KtorExposedIntegrationResources(
                dataSource = dataSource,
                r2dbcPool = r2dbcPool,
                jdbcDatabase = jdbcDatabase,
                r2dbcDatabase = r2dbcDatabase,
                jdbcDispatcher = jdbcDispatcher,
            )
            resources.initializeSchema()
            return resources
        }
    }

    private fun initializeSchema() {
        transaction(db = jdbcDatabase) {
            SchemaUtils.create(WorkshopNotes)
        }
    }
}

internal fun Application.installKtorExposedIntegrationWorkshop(resources: KtorExposedIntegrationResources) {
    monitor.subscribe(ApplicationStopped) {
        resources.close()
    }

    installBluetape4kKtorCore(
        Bluetape4kKtorCoreConfig(
            installStatusPages = false,
            installHealthRoutes = false,
        )
    )
    install(StatusPages) {
        bluetape4kErrorResponses()
        bluetape4kExposedErrors()
    }
    installBluetape4kExposedKtor(
        Bluetape4kExposedKtorConfig(
            jdbcDatabase = resources.jdbcDatabase,
            jdbcBlockingDispatcher = resources.jdbcDispatcher,
            r2dbcDatabase = resources.r2dbcDatabase,
            installHealthRoutes = true,
            readinessProbeTimeout = 2.seconds,
        )
    )

    routing {
        route("/api/notes") {
            post {
                val request = call.receive<NoteRequest>()
                val note = call.exposedJdbcTransaction(
                    db = resources.jdbcDatabase,
                    blockingDispatcher = resources.jdbcDispatcher,
                ) {
                    request.validate()
                    val id = WorkshopNotes.insertAndGetId {
                        it[title] = request.title.trim()
                        it[body] = request.body.trim()
                    }
                    WorkshopNotes
                        .selectAll()
                        .where { WorkshopNotes.id eq id }
                        .single()
                        .toNoteResponse()
                }
                call.respond(HttpStatusCode.Created, note)
            }
            get {
                val notes = call.exposedJdbcTransaction(
                    db = resources.jdbcDatabase,
                    blockingDispatcher = resources.jdbcDispatcher,
                ) {
                    WorkshopNotes
                        .selectAll()
                        .orderBy(WorkshopNotes.id to SortOrder.ASC)
                        .map { it.toNoteResponse() }
                }
                call.respond(notes)
            }
        }
        get("/api/failures/sql") {
            throw SQLException(
                "jdbc:h2:mem:secret; password=top-secret; select * from notes"
            )
        }
    }
}

fun main() {
    val resources = KtorExposedIntegrationResources.create()
    embeddedServer(CIO, port = 8080) {
        installKtorExposedIntegrationWorkshop(resources)
    }.start(wait = true)
}

private fun NoteRequest.validate() {
    require(title.isNotBlank()) { "title must not be blank" }
    require(body.isNotBlank()) { "body must not be blank" }
}

private fun ResultRow.toNoteResponse(): NoteResponse =
    NoteResponse(
        id = this[WorkshopNotes.id].value,
        title = this[WorkshopNotes.title],
        body = this[WorkshopNotes.body],
    )
