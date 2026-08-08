package exposed.shared.tests

import io.bluetape4k.assertions.shouldBeEqualTo
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class KtorTestSupportTest {

    @Test
    fun `shared client decodes json response`() = testApplication {
        application {
            routing {
                get("/") {
                    call.respondText("{\"message\":\"ok\"}", ContentType.Application.Json)
                }
            }
        }

        val response = createJsonClient().get("/").body<JsonObject>()

        response.getValue("message").jsonPrimitive.content shouldBeEqualTo "ok"
    }
}
