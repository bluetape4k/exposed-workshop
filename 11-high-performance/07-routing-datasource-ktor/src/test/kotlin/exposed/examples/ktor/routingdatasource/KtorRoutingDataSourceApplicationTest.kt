package exposed.examples.ktor.routingdatasource

import exposed.examples.ktor.routingdatasource.model.InventoryResponse
import exposed.examples.ktor.routingdatasource.model.RoutingStatsResponse
import exposed.examples.ktor.routingdatasource.routing.DataSourceRole
import io.bluetape4k.assertions.shouldBeEqualTo
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorRoutingDataSourceApplicationTest {

    @Test
    fun `get routes default to read datasource`() = testApplication {
        application {
            ktorRoutingDataSourceModule()
        }
        val client = createJsonClient()

        val response = client.get("/inventory/sku-1").body<InventoryResponse>()

        response.selectedDataSource shouldBeEqualTo DataSourceRole.READ
        response.quantity shouldBeEqualTo 10
    }

    @Test
    fun `write routes default to write datasource`() = testApplication {
        application {
            ktorRoutingDataSourceModule()
        }
        val client = createJsonClient()

        val response = client.put("/inventory/sku-1") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"quantity":321}""")
        }.body<InventoryResponse>()

        response.selectedDataSource shouldBeEqualTo DataSourceRole.WRITE
        response.quantity shouldBeEqualTo 321
    }

    @Test
    fun `header can route representative reads to write datasource`() = testApplication {
        application {
            ktorRoutingDataSourceModule()
        }
        val client = createJsonClient()

        val response = client.get("/inventory/sku-1") {
            header(DataSourceRole.HEADER_NAME, "write")
        }.body<InventoryResponse>()

        response.selectedDataSource shouldBeEqualTo DataSourceRole.WRITE
        response.quantity shouldBeEqualTo 100
    }

    @Test
    fun `invalid datasource header is rejected`() = testApplication {
        application {
            ktorRoutingDataSourceModule()
        }

        val response = client.get("/inventory/sku-1") {
            header(DataSourceRole.HEADER_NAME, "archive")
        }

        response.status shouldBeEqualTo HttpStatusCode.BadRequest
    }

    @Test
    fun `routing stats expose selected datasource counts`() = testApplication {
        application {
            ktorRoutingDataSourceModule()
        }
        val client = createJsonClient()

        client.get("/inventory/sku-1").status shouldBeEqualTo HttpStatusCode.OK
        client.get("/inventory/sku-1") {
            header(DataSourceRole.HEADER_NAME, "write")
        }.status shouldBeEqualTo HttpStatusCode.OK
        val stats = client.get("/routing/stats").body<RoutingStatsResponse>()

        stats.readSelections shouldBeEqualTo 1
        stats.writeSelections shouldBeEqualTo 1
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
