package exposed.examples.ktor.cache

import exposed.examples.ktor.cache.model.CacheStatsResponse
import exposed.examples.ktor.cache.model.UserResponse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
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
