package exposed.multitenant.ktor

import exposed.multitenant.ktor.model.MovieResponse
import exposed.multitenant.ktor.tenant.Tenant
import exposed.multitenant.ktor.tenant.TenantContext
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantKtorApplicationTest {

    @Test
    fun `returns tenant-specific movies`() = testApplication {
        application {
            tenantKtorModule()
        }
        val client = createJsonClient()

        val acmeMovies = client.get("/movies") {
            header(Tenant.HEADER_NAME, "acme")
        }.body<List<MovieResponse>>()

        val globexMovies = client.get("/movies") {
            header(Tenant.HEADER_NAME, "globex")
        }.body<List<MovieResponse>>()

        acmeMovies.map { it.title }.joinToString() shouldContain "ACME"
        globexMovies.map { it.title }.joinToString() shouldContain "Globex"
    }

    @Test
    fun `rejects missing tenant header`() = testApplication {
        application {
            tenantKtorModule()
        }

        val response = client.get("/movies")

        response.status shouldBeEqualTo HttpStatusCode.BadRequest
    }

    @Test
    fun `rejects unknown tenant header`() = testApplication {
        application {
            tenantKtorModule()
        }

        val response = client.get("/movies") {
            header(Tenant.HEADER_NAME, "unknown")
        }

        response.status shouldBeEqualTo HttpStatusCode.BadRequest
    }

    @Test
    fun `isolates writes between tenants and clears context`() = testApplication {
        application {
            tenantKtorModule()
        }
        val client = createJsonClient()

        client.post("/movies") {
            header(Tenant.HEADER_NAME, "acme")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"title":"ACME private release"}""")
        }.status shouldBeEqualTo HttpStatusCode.Created

        val acmeMovies = client.get("/movies") {
            header(Tenant.HEADER_NAME, "acme")
        }.body<List<MovieResponse>>()
        val globexMovies = client.get("/movies") {
            header(Tenant.HEADER_NAME, "globex")
        }.body<List<MovieResponse>>()

        acmeMovies.map { it.title } shouldContain "ACME private release"
        globexMovies.map { it.title } shouldNotContain "ACME private release"
        TenantContext.currentTenantOrNull() shouldBeEqualTo null
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.createJsonClient() =
        createClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
        }
}
