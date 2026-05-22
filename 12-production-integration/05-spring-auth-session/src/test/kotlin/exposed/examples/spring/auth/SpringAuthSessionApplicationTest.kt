package exposed.examples.spring.auth

import exposed.examples.spring.auth.model.ProfileResponse
import exposed.examples.spring.auth.model.SessionResponse
import exposed.examples.spring.auth.model.SessionsResponse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import java.util.Base64
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(
    classes = [SpringAuthSessionApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SpringAuthSessionApplicationTest(
    @param:Autowired private val client: WebTestClient,
) {

    @Test
    fun `missing and invalid credentials are rejected`() {
        client.get()
            .uri("/api/profile")
            .exchange()
            .expectStatus().isUnauthorized

        client.get()
            .uri("/api/profile")
            .basic("alice", "wrong")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `user credentials can read profile but cannot access admin endpoint`() {
        val profile = client.get()
            .uri("/api/profile")
            .basic("alice", "password")
            .exchange()
            .expectStatus().isOk
            .expectBody<ProfileResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        profile.username shouldBeEqualTo "alice"
        profile.roles shouldContain "USER"

        client.get()
            .uri("/api/admin")
            .basic("alice", "password")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `admin credentials can access admin endpoint`() {
        client.get()
            .uri("/api/admin")
            .basic("admin", "password")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.username").isEqualTo("admin")
    }

    @Test
    fun `authenticated users can persist and list session metadata`() {
        val created = client.post()
            .uri("/api/sessions")
            .basic("alice", "password")
            .exchange()
            .expectStatus().isOk
            .expectBody<SessionResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        created.username shouldBeEqualTo "alice"
        created.token.shouldNotBeNull()

        val sessions = client.get()
            .uri("/api/sessions")
            .basic("alice", "password")
            .exchange()
            .expectStatus().isOk
            .expectBody<SessionsResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        sessions.sessions.any { session ->
            session.username == "alice" &&
                session.token == null &&
                session.expiresAt.isAfter(session.issuedAt)
        } shouldBeEqualTo true
    }

    @Test
    fun `session creation returns distinct opaque tokens`() {
        val first = client.post()
            .uri("/api/sessions")
            .basic("alice", "password")
            .exchange()
            .expectStatus().isOk
            .expectBody<SessionResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        val second = client.post()
            .uri("/api/sessions")
            .basic("alice", "password")
            .exchange()
            .expectStatus().isOk
            .expectBody<SessionResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        first.token.shouldNotBeNull()
        second.token.shouldNotBeNull()
        (first.token != second.token) shouldBeEqualTo true
    }

    private fun WebTestClient.RequestHeadersSpec<*>.basic(username: String, password: String) =
        header("Authorization", "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray()))
}
