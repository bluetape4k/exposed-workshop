package exposed.examples.spring.realtime

import exposed.examples.spring.realtime.model.CreateNotificationRequest
import exposed.examples.spring.realtime.model.RealtimeEvent
import exposed.examples.spring.realtime.persistence.NotificationPersistence
import exposed.examples.spring.realtime.repository.ExposedNotificationOutboxRepository
import exposed.examples.spring.realtime.service.NotificationService
import exposed.examples.spring.realtime.service.RealtimeDelivery
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

@SpringBootTest(classes = [SpringOutboxRealtimeApplication::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringOutboxRealtimeApplicationTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    private lateinit var client: WebTestClient

    @Autowired
    private lateinit var service: NotificationService

    @BeforeEach
    fun resetState() {
        client = WebTestClient.bindToApplicationContext(applicationContext)
            .configureClient()
            .build()
        service.deleteAll()
    }

    @Test
    fun `index and health endpoints respond`() {
        client.get().uri("/")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.service").isEqualTo("spring-outbox-realtime")

        client.get().uri("/health")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
    }

    @Test
    fun `notification is persisted before publish and delivery marks outbox published`() {
        client.post().uri("/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"recipientId":"user-1","message":"hello"}""")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("PENDING")

        client.post().uri("/outbox/publish")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.attempted").isEqualTo(1)
            .jsonPath("$.delivered").isEqualTo(1)
            .jsonPath("$.failed").isEqualTo(0)

        client.get().uri("/outbox")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.events.length()").isEqualTo(1)
            .jsonPath("$.events[0].status").isEqualTo("PUBLISHED")
    }

    @Test
    fun `replay after boundary returns only later published events`() {
        postNotification("user-1", "first")
        postNotification("user-2", "second")
        client.post().uri("/outbox/publish").exchange().expectStatus().isOk

        val firstEventId = service.outbox().events.first().eventId
        service.replayAfter(firstEventId).map { it.message } shouldBeEqualTo listOf("second")
    }

    @Test
    fun `sse endpoint replays boundary once and then streams live events`() {
        postNotification("user-1", "first")
        postNotification("user-2", "second")
        client.post().uri("/outbox/publish").exchange().expectStatus().isOk

        val firstEventId = service.outbox().events.first().eventId
        val result = client.get().uri("/events?after=$firstEventId")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk
            .returnResult(RealtimeEvent::class.java)

        postNotification("user-3", "third")
        client.post().uri("/outbox/publish").exchange().expectStatus().isOk

        val messages = result.responseBody
            .take(2)
            .map { it.message }
            .collectList()
            .block(Duration.ofSeconds(5))

        messages shouldBeEqualTo listOf("second", "third")
    }

    @Test
    fun `delivery failure is recorded without losing the outbox event`() {
        val persistence = NotificationPersistence.inMemory("spring_failure_test")
        try {
            val failingService = NotificationService(
                ExposedNotificationOutboxRepository(persistence.database),
                FailingRealtimeDelivery
            )
            failingService.create(
                CreateNotificationRequest(
                    recipientId = "user-1",
                    message = "will fail"
                )
            )

            val result = failingService.publishPending()
            result.attempted shouldBeEqualTo 1
            result.delivered shouldBeEqualTo 0
            result.failed shouldBeEqualTo 1

            failingService.outbox().events.single().status.name shouldBeEqualTo "FAILED"
        } finally {
            persistence.close()
        }
    }

    private fun postNotification(recipientId: String, message: String) {
        client.post().uri("/notifications")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"recipientId":"$recipientId","message":"$message"}""")
            .exchange()
            .expectStatus().isOk
    }

    private object FailingRealtimeDelivery : RealtimeDelivery {
        override fun deliver(event: RealtimeEvent): Boolean = false
    }
}
