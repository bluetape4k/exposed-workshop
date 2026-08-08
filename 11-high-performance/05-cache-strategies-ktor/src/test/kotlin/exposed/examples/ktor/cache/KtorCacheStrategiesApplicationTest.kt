package exposed.examples.ktor.cache

import exposed.examples.ktor.cache.model.CacheStatsResponse
import exposed.examples.ktor.cache.model.UserResponse
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
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class KtorCacheStrategiesApplicationTest {

    @Test
    fun `cache aside populates cache after first database fallback`() = testApplication {
        application {
            ktorCacheStrategiesModule()
        }
        val client = createJsonClient()

        val first = client.get("/users/u1/cache-aside").body<UserResponse>()
        val second = client.get("/users/u1/cache-aside").body<UserResponse>()
        val stats = client.get("/cache/stats").body<CacheStatsResponse>()

        first.source shouldBeEqualTo "cache-aside-database"
        second.source shouldBeEqualTo "cache-aside-cache"
        stats.databaseReads shouldBeEqualTo 1
        stats.cacheHits shouldBeEqualTo 1
        stats.cacheMisses shouldBeEqualTo 1
    }

    @Test
    fun `write through updates database and cache together`() = testApplication {
        application {
            ktorCacheStrategiesModule()
        }
        val client = createJsonClient()

        client.put("/users/u1/write-through") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody("""{"displayName":"Ada Byron"}""")
        }.status shouldBeEqualTo HttpStatusCode.OK

        val cached = client.get("/users/u1/read-through").body<UserResponse>()

        cached.displayName shouldBeEqualTo "Ada Byron"
        cached.source shouldBeEqualTo "read-through-cache"
    }

    @Test
    fun `invalidate forces next read to fall back to database`() = testApplication {
        application {
            ktorCacheStrategiesModule()
        }
        val client = createJsonClient()

        client.get("/users/u2/cache-aside").status shouldBeEqualTo HttpStatusCode.OK
        client.delete("/users/u2/cache").status shouldBeEqualTo HttpStatusCode.NoContent
        val afterInvalidate = client.get("/users/u2/cache-aside").body<UserResponse>()
        val stats = client.get("/cache/stats").body<CacheStatsResponse>()

        afterInvalidate.source shouldBeEqualTo "cache-aside-database"
        stats.databaseReads shouldBeEqualTo 2
        stats.cacheMisses shouldBeEqualTo 2
    }

    @Test
    fun `library health routes and legacy health contract are both available`() {
        val previousDefault = TransactionManager.defaultDatabase
        testApplication {
            application {
                ktorCacheStrategiesModule()
            }
            val client = createJsonClient()

            client.get("/health").body<exposed.examples.ktor.cache.model.HealthResponse>().status shouldBeEqualTo "UP"
            val health = client.get("/healthz/exposed")
            health.status shouldBeEqualTo HttpStatusCode.OK
            health.bodyAsText().contains("\"status\":\"UP\"") shouldBeEqualTo true

            val readiness = client.get("/ready")
            readiness.status shouldBeEqualTo HttpStatusCode.OK
            readiness.bodyAsText().contains("cache.users-write") shouldBeEqualTo true

            client.delete("/users/unknown/cache").status shouldBeEqualTo HttpStatusCode.NotFound
        }
        TransactionManager.defaultDatabase shouldBeEqualTo previousDefault
    }

}
