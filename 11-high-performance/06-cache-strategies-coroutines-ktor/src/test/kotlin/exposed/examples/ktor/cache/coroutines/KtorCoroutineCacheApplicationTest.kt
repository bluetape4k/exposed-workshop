package exposed.examples.ktor.cache.coroutines

import exposed.examples.ktor.cache.coroutines.model.CoroutineCacheStatsResponse
import exposed.examples.ktor.cache.coroutines.model.HealthResponse
import exposed.examples.ktor.cache.coroutines.model.ProductResponse
import exposed.shared.tests.createJsonClient
import io.bluetape4k.assertions.shouldBeEqualTo
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class KtorCoroutineCacheApplicationTest {

    @Test
    fun `read through serves later requests from coroutine-safe cache`() = testApplication {
        application {
            ktorCoroutineCacheModule()
        }
        val client = createJsonClient()

        val first = client.get("/products/sku-1/read-through").body<ProductResponse>()
        val second = client.get("/products/sku-1/read-through").body<ProductResponse>()
        val stats = client.get("/cache/stats").body<CoroutineCacheStatsResponse>()

        first.source shouldBeEqualTo "read-through-database"
        second.source shouldBeEqualTo "read-through-cache"
        stats.databaseReads shouldBeEqualTo 1
        stats.cacheHits shouldBeEqualTo 1
        stats.cacheMisses shouldBeEqualTo 1
    }

    @Test
    fun `concurrent read through requests coalesce one database load`() = testApplication {
        application {
            ktorCoroutineCacheModule()
        }
        val client = createJsonClient()

        coroutineScope {
            (1..8)
                .map {
                    async {
                        client.get("/products/sku-2/read-through").body<ProductResponse>()
                    }
                }
                .awaitAll()
        }
        val stats = client.get("/cache/stats").body<CoroutineCacheStatsResponse>()

        stats.databaseReads shouldBeEqualTo 1
        stats.inFlightLoads shouldBeEqualTo 0
    }

    @Test
    fun `write through updates cache for suspend route handlers`() = testApplication {
        application {
            ktorCoroutineCacheModule()
        }
        val client = createJsonClient()

        client.put("/products/sku-1/write-through") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"name":"Coroutine cache updated"}""")
        }.status shouldBeEqualTo HttpStatusCode.OK

        val cached = client.get("/products/sku-1/read-through").body<ProductResponse>()

        cached.name shouldBeEqualTo "Coroutine cache updated"
        cached.source shouldBeEqualTo "read-through-cache"
    }

    @Test
    fun `invalidation makes the next suspend read hit database again`() = testApplication {
        application {
            ktorCoroutineCacheModule()
        }
        val client = createJsonClient()

        client.get("/products/sku-1/read-through").status shouldBeEqualTo HttpStatusCode.OK
        client.delete("/products/sku-1/cache").status shouldBeEqualTo HttpStatusCode.NoContent
        client.get("/products/sku-1/read-through").status shouldBeEqualTo HttpStatusCode.OK
        val stats = client.get("/cache/stats").body<CoroutineCacheStatsResponse>()

        stats.databaseReads shouldBeEqualTo 2
        stats.cacheMisses shouldBeEqualTo 2
    }

    @Test
    fun `library health routes and legacy health contract are both available`() {
        val previousDefault = TransactionManager.defaultDatabase
        testApplication {
            application {
                ktorCoroutineCacheModule()
            }
            val client = createJsonClient()

            client.get("/health").body<HealthResponse>().status shouldBeEqualTo "UP"
            val health = client.get("/healthz/exposed")
            health.status shouldBeEqualTo HttpStatusCode.OK
            health.bodyAsText().contains("\"status\":\"UP\"") shouldBeEqualTo true

            val readiness = client.get("/ready")
            readiness.status shouldBeEqualTo HttpStatusCode.OK
            readiness.bodyAsText().contains("cache.products-cache") shouldBeEqualTo true

            client.delete("/products/unknown/cache").status shouldBeEqualTo HttpStatusCode.NotFound
        }
        TransactionManager.defaultDatabase shouldBeEqualTo previousDefault
    }

}
