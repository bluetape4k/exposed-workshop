package exposed.examples.ktor.realtime

import exposed.examples.ktor.realtime.model.HealthResponse
import exposed.examples.ktor.realtime.model.RealtimeEvent
import exposed.examples.ktor.realtime.persistence.NotificationPersistence
import exposed.examples.ktor.realtime.service.RealtimeDelivery
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.codec.Base58
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtorOutboxRealtimeApplicationTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `index and health endpoints respond`() = testApplication {
        application {
            ktorOutboxRealtimeModule(
                persistence = NotificationPersistence.inMemory("app_${Base58.randomString(8)}")
            )
        }

        val index = client.get("/")
        index.status shouldBeEqualTo HttpStatusCode.OK
        index.bodyAsText() shouldContain "ktor-outbox-realtime"

        val health = json.decodeFromString<HealthResponse>(client.get("/health").bodyAsText())
        health.status shouldBeEqualTo "UP"
    }

    @Test
    fun `websocket replay sends published events after reconnect boundary`() = testApplication {
        application {
            ktorOutboxRealtimeModule(
                persistence = NotificationPersistence.inMemory("replay_${Base58.randomString(8)}")
            )
        }
        val realtimeClient = createClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets)
        }

        postNotification("user-1", "first")
        postNotification("user-2", "second")
        client.post("/outbox/publish")

        realtimeClient.webSocket("/events?after=0") {
            val first = json.decodeFromString<RealtimeEvent>((incoming.receive() as Frame.Text).readText())
            val second = json.decodeFromString<RealtimeEvent>((incoming.receive() as Frame.Text).readText())

            first.message shouldBeEqualTo "first"
            second.message shouldBeEqualTo "second"
        }

        realtimeClient.webSocket("/events?after=1") {
            val replayed = json.decodeFromString<RealtimeEvent>((incoming.receive() as Frame.Text).readText())
            replayed.message shouldBeEqualTo "second"
        }
    }

    @Test
    fun `websocket streams live event published after subscription`() = testApplication {
        application {
            ktorOutboxRealtimeModule(
                persistence = NotificationPersistence.inMemory("live_${Base58.randomString(8)}")
            )
        }
        val realtimeClient = createClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets)
        }

        postNotification("user-3", "live")

        realtimeClient.webSocket("/events?after=0") {
            client.post("/outbox/publish").status shouldBeEqualTo HttpStatusCode.OK

            val frame = withTimeout(5_000) {
                incoming.receive() as Frame.Text
            }
            val event = json.decodeFromString<RealtimeEvent>(frame.readText())
            event.message shouldBeEqualTo "live"
        }
    }

    @Test
    fun `delivery failure is recorded without losing the outbox event`() = testApplication {
        val delivery = FailingRealtimeDelivery()
        application {
            ktorOutboxRealtimeModule(
                persistence = NotificationPersistence.inMemory("failure_${Base58.randomString(8)}"),
                realtimeDelivery = delivery
            )
        }

        postNotification("user-1", "will fail")

        val publish = client.post("/outbox/publish")
        publish.status shouldBeEqualTo HttpStatusCode.OK
        publish.bodyAsText() shouldContain "\"failed\": 1"

        val outbox = client.get("/outbox").bodyAsText()
        outbox shouldContain "\"status\": \"FAILED\""
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.postNotification(
        recipientId: String,
        message: String,
    ) {
        client.post("/notifications") {
            contentType(ContentType.Application.Json)
            setBody("""{"recipientId":"$recipientId","message":"$message"}""")
        }.status shouldBeEqualTo HttpStatusCode.OK
    }
}

internal class FailingRealtimeDelivery : RealtimeDelivery {
    override fun deliver(event: RealtimeEvent): Boolean = false
}
