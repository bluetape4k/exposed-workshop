package exposed.examples.spring.auth

import exposed.examples.spring.auth.model.ProfileResponse
import exposed.examples.spring.auth.model.SessionResponse
import exposed.examples.spring.auth.model.SessionsResponse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.tink.digest.TinkDigesters
import java.util.Base64
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
    @param:Autowired private val database: Database,
) {

    @Test
    fun `tink SHA-256 hex preserves UTF-8 lowercase 64-character contract`() {
        TinkDigesters.SHA256.digestHex("") shouldBeEqualTo
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        TinkDigesters.SHA256.digestHex("한글🔐") shouldBeEqualTo
            "4bf6e938ebc1c053c46bb45c34671c82ba63137812782261f5c709cf6766ee11"
        TinkDigesters.SHA256.digestHex("286") shouldBeEqualTo
            "00328ce57bbc14b33bd6695bc8eb32cdf2fb5f3a7d89ec14a42825e15d39df60"
    }

    @Test
    fun `tink SHA-256 hex verifier accepts only canonical matching hashes`() {
        val token = "286"
        val expectedHash = "00328ce57bbc14b33bd6695bc8eb32cdf2fb5f3a7d89ec14a42825e15d39df60"
        val wrongHash = "10328ce57bbc14b33bd6695bc8eb32cdf2fb5f3a7d89ec14a42825e15d39df60"

        TinkDigesters.SHA256.matchesHex(token, expectedHash) shouldBeEqualTo true
        TinkDigesters.SHA256.matchesHex(token, wrongHash) shouldBeEqualTo false
        TinkDigesters.SHA256.matchesHex(token, expectedHash.uppercase()) shouldBeEqualTo false
        TinkDigesters.SHA256.matchesHex(token, "not-a-hex-digest") shouldBeEqualTo false
        TinkDigesters.SHA256.matchesHex(token, expectedHash.dropLast(2)) shouldBeEqualTo false
    }

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
        val token = created.token.shouldNotBeNull()

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

        val expectedHash = TinkDigesters.SHA256.digestHex(token)
        val storedHash = transaction(database) {
            AuthSessionsProbe.selectAll()
                .where { AuthSessionsProbe.tokenHash eq expectedHash }
                .single()[AuthSessionsProbe.tokenHash]
        }
        storedHash shouldBeEqualTo expectedHash
        storedHash.length shouldBeEqualTo 64
        storedHash shouldBeEqualTo storedHash.lowercase()
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

private object AuthSessionsProbe : Table("auth_sessions") {
    val tokenHash = varchar("token_hash", 64)
}
