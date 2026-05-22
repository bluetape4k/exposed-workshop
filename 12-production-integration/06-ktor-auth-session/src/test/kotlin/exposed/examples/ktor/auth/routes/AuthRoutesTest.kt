package exposed.examples.ktor.auth.routes

import exposed.examples.ktor.auth.ktorAuthSessionModule
import exposed.examples.ktor.auth.model.ProfileResponse
import exposed.examples.ktor.auth.model.SessionResponse
import exposed.examples.ktor.auth.model.SessionsResponse
import exposed.examples.ktor.auth.persistence.AuthPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.Base64
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthRoutesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `missing and invalid credentials are rejected`() = testApplicationWithPersistence {
        client.get("/api/profile").status shouldBeEqualTo HttpStatusCode.Unauthorized
        client.get("/api/profile") {
            basic("alice", "wrong")
        }.status shouldBeEqualTo HttpStatusCode.Unauthorized
    }

    @Test
    fun `user credentials can read profile but cannot access admin endpoint`() = testApplicationWithPersistence {
        val profileResponse = client.get("/api/profile") {
            basic("alice", "password")
        }
        profileResponse.status shouldBeEqualTo HttpStatusCode.OK

        val profile = json.decodeFromString<ProfileResponse>(profileResponse.bodyAsText())
        profile.username shouldBeEqualTo "alice"
        profile.roles shouldContain "USER"

        val adminResponse = client.get("/api/admin") {
            basic("alice", "password")
        }
        adminResponse.status shouldBeEqualTo HttpStatusCode.Forbidden
    }

    @Test
    fun `admin credentials can access admin endpoint`() = testApplicationWithPersistence {
        val adminResponse = client.get("/api/admin") {
            basic("admin", "password")
        }
        adminResponse.status shouldBeEqualTo HttpStatusCode.OK

        val profile = json.decodeFromString<ProfileResponse>(adminResponse.bodyAsText())
        profile.username shouldBeEqualTo "admin"
        profile.roles shouldContain "ADMIN"
    }

    @Test
    fun `authenticated users can persist and list session metadata`() = testApplicationWithPersistence {
        val createResponse = client.post("/api/sessions") {
            basic("alice", "password")
        }
        createResponse.status shouldBeEqualTo HttpStatusCode.OK
        createResponse.headers[HttpHeaders.SetCookie].shouldNotBeNull()

        val created = json.decodeFromString<SessionResponse>(createResponse.bodyAsText())
        created.username shouldBeEqualTo "alice"
        created.token.shouldNotBeNull()

        val listResponse = client.get("/api/sessions") {
            basic("alice", "password")
        }
        listResponse.status shouldBeEqualTo HttpStatusCode.OK

        val sessions = json.decodeFromString<SessionsResponse>(listResponse.bodyAsText())
        val listed = sessions.sessions.single { it.username == "alice" }
        listed.token.shouldBeNull()
        (listed.expiresAtEpochMs > listed.issuedAtEpochMs) shouldBeEqualTo true
    }

    @Test
    fun `session cookie can load the current profile without basic credentials`() = testApplicationWithPersistence {
        client.get("/api/session-profile").status shouldBeEqualTo HttpStatusCode.Unauthorized

        val createResponse = client.post("/api/sessions") {
            basic("alice", "password")
        }
        createResponse.status shouldBeEqualTo HttpStatusCode.OK

        val setCookie = createResponse.headers[HttpHeaders.SetCookie].shouldNotBeNull()
        val cookiePair = setCookie.substringBefore(";")
        val profileResponse = client.get("/api/session-profile") {
            header(HttpHeaders.Cookie, cookiePair)
        }
        profileResponse.status shouldBeEqualTo HttpStatusCode.OK

        val profile = json.decodeFromString<ProfileResponse>(profileResponse.bodyAsText())
        profile.username shouldBeEqualTo "alice"
        profile.roles shouldContain "USER"
    }

    @Test
    fun `invalid session cookie is rejected as unauthorized`() = testApplicationWithPersistence {
        client.get("/api/session-profile") {
            header(HttpHeaders.Cookie, "auth_session=invalid")
        }.status shouldBeEqualTo HttpStatusCode.Unauthorized
    }

    private fun testApplicationWithPersistence(
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) = testApplication {
        val persistence = AuthPersistence.inMemory("routes_${Base58.randomString(8)}")

        application {
            ktorAuthSessionModule(persistence)
        }

        test()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.basic(username: String, password: String) {
        val encoded = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        header(HttpHeaders.Authorization, "Basic $encoded")
    }
}
