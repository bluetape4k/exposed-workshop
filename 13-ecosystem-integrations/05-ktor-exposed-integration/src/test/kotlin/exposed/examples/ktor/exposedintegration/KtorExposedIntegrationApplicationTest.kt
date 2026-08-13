package exposed.examples.ktor.exposedintegration

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.ktor.core.HealthResponse
import io.bluetape4k.ktor.testing.bluetape4kJsonClient
import io.bluetape4k.ktor.testing.decodeJsonBody
import io.bluetape4k.ktor.testing.shouldHaveStatus
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorExposedIntegrationApplicationTest {

    @Test
    fun `helper backed jdbc transaction creates and lists notes`() = testApplication {
        KtorExposedIntegrationResources.create("crud").use { resources ->
            application {
                installKtorExposedIntegrationWorkshop(resources)
            }

            val jsonClient = bluetape4kJsonClient()
            val created = jsonClient.post("/api/notes") {
                contentType(ContentType.Application.Json)
                setBody(NoteRequest(title = "helper boundary", body = "JDBC transaction is owned by the helper."))
            }.shouldHaveStatus(HttpStatusCode.Created)
                .decodeJsonBody<NoteResponse>()

            created.title shouldBeEqualTo "helper boundary"
            created.body shouldBeEqualTo "JDBC transaction is owned by the helper."

            val notes = jsonClient.get("/api/notes")
                .shouldHaveStatus(HttpStatusCode.OK)
                .decodeJsonBody<List<NoteResponse>>()

            notes.map { it.id } shouldContain created.id
        }
    }

    @Test
    fun `helper exposes liveness and readiness for caller owned resources`() = testApplication {
        KtorExposedIntegrationResources.create("readiness").use { resources ->
            application {
                installKtorExposedIntegrationWorkshop(resources)
            }

            val jsonClient = bluetape4kJsonClient()

            val health = jsonClient.get("/healthz/exposed")
                .shouldHaveStatus(HttpStatusCode.OK)
                .decodeJsonBody<HealthResponse>()
            health shouldBeEqualTo HealthResponse.up(mapOf("exposed" to HealthResponse.UP))

            val readiness = jsonClient.get("/readyz/exposed")
                .shouldHaveStatus(HttpStatusCode.OK)
                .decodeJsonBody<HealthResponse>()
            readiness shouldBeEqualTo HealthResponse.up(
                mapOf(
                    "jdbc" to HealthResponse.UP,
                    "r2dbc" to HealthResponse.UP,
                )
            )
        }
    }

    @Test
    fun `helper readiness returns service unavailable when jdbc resource fails`() = testApplication {
        KtorExposedIntegrationResources.create("readiness-failure").use { resources ->
            application {
                installKtorExposedIntegrationWorkshop(resources)
            }
            resources.closeJdbcDataSourceOnly()

            val response = bluetape4kJsonClient().get("/readyz/exposed")
                .shouldHaveStatus(HttpStatusCode.ServiceUnavailable)
                .bodyAsText()

            response shouldContain "DOWN"
            response shouldContain "jdbc"
            response shouldNotContain "jdbc:h2:"
        }
    }

    @Test
    fun `r2dbc helper preserves pool bounds and idempotent cleanup`() {
        val resources = KtorExposedIntegrationResources.create("pool-helper")
        try {
            resources.r2dbcPool.warmup().block()

            val metrics = resources.r2dbcPool.metrics.orElseThrow()
            metrics.maxAllocatedSize shouldBeEqualTo 2
            metrics.allocatedSize() shouldBeEqualTo 1
            resources.r2dbcPool.isDisposed shouldBeEqualTo false
        } finally {
            resources.close()
            resources.close()
        }

        resources.r2dbcPool.isDisposed shouldBeEqualTo true
    }

    @Test
    fun `application stopped event disposes caller owned pool`() = testApplication {
        val resources = KtorExposedIntegrationResources.create("application-stopped")
        application {
            installKtorExposedIntegrationWorkshop(resources)
        }
        startApplication()

        application.monitor.raise(ApplicationStopped, application)

        resources.r2dbcPool.isDisposed shouldBeEqualTo true
        resources.close()
    }

    @Test
    fun `exposed status pages sanitize database failures`() = testApplication {
        KtorExposedIntegrationResources.create("status-pages").use { resources ->
            application {
                installKtorExposedIntegrationWorkshop(resources)
            }

            val response = bluetape4kJsonClient().get("/api/failures/sql")
                .shouldHaveStatus(HttpStatusCode.ServiceUnavailable)
                .bodyAsText()

            response shouldContain "EXPOSED_DATABASE_UNAVAILABLE"
            response shouldNotContain "jdbc:h2:"
            response shouldNotContain "password"
            response shouldNotContain "select * from notes"
        }
    }
}
