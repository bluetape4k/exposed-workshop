package exposed.examples.ktor.architecture

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private const val DEFAULT_PORT = 8080
private const val MAX_REQUEST_BODY_BYTES = 64 * 1024L
private const val REQUEST_BODY_BUFFER_BYTES = 8 * 1024
private const val HIKARI_MAX_POOL_SIZE = 4

private val ApplicationJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private object KtorArchitectureLogger : KLogging()

/**
 * Starts the Ktor architecture example with an in-memory H2 database.
 *
 * ## Contract
 * - The route layer stays thin and delegates to a service.
 * - The repository owns every blocking Exposed JDBC transaction behind
 *   `Dispatchers.IO`.
 * - JSON errors are mapped to sanitized responses through `StatusPages`.
 */
fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT) {
        ktorArchitectureModule()
    }.start(wait = true)
}

/**
 * Configures the Ktor + Exposed architecture example.
 *
 * The default persistence uses an in-memory H2 database for local execution.
 * Tests pass a dedicated persistence instance with a unique JDBC URL per test.
 */
internal fun Application.ktorArchitectureModule(
    persistence: CustomerPersistence = CustomerPersistence.inMemory(),
) {
    monitor.subscribe(ApplicationStopped) {
        persistence.close()
    }

    installKtorPlugins()

    val repository = ExposedCustomerRepository(persistence.database)
    val service = CustomerService(repository)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        customerRoutes(service)
    }
}

private fun Application.installKtorPlugins() {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        callIdMdc("callId")
    }
    install(ContentNegotiation) {
        json(ApplicationJson)
    }
    install(StatusPages) {
        exception<PayloadTooLargeException> { call, _ ->
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                ErrorResponse(code = "PAYLOAD_TOO_LARGE", message = "Request body is too large")
            )
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "BAD_REQUEST", message = "Malformed request body")
            )
        }
        exception<CustomerValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "VALIDATION_FAILED", message = cause.message ?: "Invalid request")
            )
        }
        exception<IllegalArgumentException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(code = "VALIDATION_FAILED", message = "Invalid request")
            )
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(code = "NOT_FOUND", message = cause.message ?: "Resource not found")
            )
        }
        exception<Exception> { call, cause ->
            if (cause is CancellationException) {
                throw cause
            }
            KtorArchitectureLogger.log.error(cause) {
                "Unhandled failure in Ktor architecture example"
            }
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(code = "INTERNAL_ERROR", message = "Internal server error")
            )
        }
    }
}

/**
 * Holds the JDBC resources owned by a Ktor application instance.
 */
internal class CustomerPersistence private constructor(
    private val dataSource: HikariDataSource,
) : AutoCloseable {

    val database: Database = Database.connect(dataSource)

    override fun close() {
        dataSource.close()
    }

    companion object {
        fun inMemory(name: String = "ktor_architecture_${UUID.randomUUID()}"): CustomerPersistence {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
                username = "sa"
                password = ""
                driverClassName = "org.h2.Driver"
                maximumPoolSize = HIKARI_MAX_POOL_SIZE
                poolName = "ktor-architecture-$name"
            }
            return CustomerPersistence(HikariDataSource(config))
        }
    }
}

/**
 * Customer persistence contract used by the service layer.
 */
internal interface CustomerRepository {
    suspend fun create(command: CreateCustomerCommand): CustomerRecord
    suspend fun findById(id: Long): CustomerRecord?
    suspend fun findAll(): List<CustomerRecord>
    suspend fun count(): Long
}

/**
 * Exposed JDBC implementation that isolates blocking work on `Dispatchers.IO`.
 */
internal class ExposedCustomerRepository(
    private val database: Database,
) : CustomerRepository {

    private val schemaReady = AtomicBoolean(false)
    private val schemaMutex = Mutex()

    override suspend fun create(command: CreateCustomerCommand): CustomerRecord {
        ensureSchema()
        return transactionIO {
            val id = Customers.insertAndGetId {
                it[name] = command.name
                it[email] = command.email
            }.value
            CustomerRecord(id = id, name = command.name, email = command.email)
        }
    }

    override suspend fun findById(id: Long): CustomerRecord? {
        ensureSchema()
        return transactionIO {
            Customers
                .selectAll()
                .where { Customers.id eq id }
                .singleOrNull()
                ?.toRecord()
        }
    }

    override suspend fun findAll(): List<CustomerRecord> {
        ensureSchema()
        return transactionIO {
            Customers
                .selectAll()
                .orderBy(Customers.id)
                .map { it.toRecord() }
        }
    }

    override suspend fun count(): Long {
        ensureSchema()
        return transactionIO {
            Customers.selectAll().count()
        }
    }

    private suspend fun ensureSchema() {
        if (schemaReady.get()) {
            return
        }
        schemaMutex.withLock {
            if (!schemaReady.get()) {
                transactionIO {
                    SchemaUtils.create(Customers)
                }
                schemaReady.set(true)
            }
        }
    }

    private suspend fun <T> transactionIO(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction(database) {
                block()
            }
        }
}

/**
 * Customer use cases and caller input validation.
 */
internal class CustomerService(
    private val repository: CustomerRepository,
) {
    suspend fun create(request: CreateCustomerRequest): CustomerResponse {
        val command = CreateCustomerCommand(
            name = request.name.normalizeName(),
            email = request.email.normalizeEmail()
        )
        return repository.create(command).toResponse()
    }

    suspend fun find(id: Long): CustomerResponse =
        repository.findById(id)?.toResponse()
            ?: throw NoSuchElementException("Customer $id was not found")

    suspend fun findAll(): CustomersResponse =
        CustomersResponse(repository.findAll().map { it.toResponse() })

    suspend fun count(): Long =
        repository.count()

    private fun String.normalizeName(): String {
        val normalized = trim()
        if (normalized.isBlank()) {
            throw CustomerValidationException("name must not be blank")
        }
        if (normalized.length > 80) {
            throw CustomerValidationException("name must be 80 characters or less")
        }
        return normalized
    }

    private fun String.normalizeEmail(): String {
        val normalized = trim().lowercase()
        if (normalized.isBlank()) {
            throw CustomerValidationException("email must not be blank")
        }
        if (normalized.length > 120) {
            throw CustomerValidationException("email must be 120 characters or less")
        }
        if ("@" !in normalized) {
            throw CustomerValidationException("email must contain @")
        }
        return normalized
    }
}

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

private suspend inline fun <reified T : Any> io.ktor.server.application.ApplicationCall.receiveLimited(): T {
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

private suspend fun io.ktor.server.application.ApplicationCall.receiveLimitedBody(): String {
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

private object Customers : LongIdTable("customers") {
    val name = varchar("name", 80)
    val email = varchar("email", 120)
}

private fun ResultRow.toRecord(): CustomerRecord =
    CustomerRecord(
        id = this[Customers.id].value,
        name = this[Customers.name],
        email = this[Customers.email]
    )

private fun CustomerRecord.toResponse(): CustomerResponse =
    CustomerResponse(id = id, name = name, email = email)

private class PayloadTooLargeException : RuntimeException()

private class CustomerValidationException(message: String) : RuntimeException(message)

@Serializable
internal data class IndexResponse(
    val service: String = "ktor-application-architecture",
    val endpoints: List<String> = listOf("/health", "/customers", "/customers/{id}"),
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class HealthResponse(
    val status: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class CreateCustomerRequest(
    val name: String,
    val email: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class CreateCustomerCommand(
    val name: String,
    val email: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal data class CustomerRecord(
    val id: Long,
    val name: String,
    val email: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class CustomerResponse(
    val id: Long,
    val name: String,
    val email: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class CustomersResponse(
    val customers: List<CustomerResponse>,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@Serializable
internal data class ErrorResponse(
    val code: String,
    val message: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
