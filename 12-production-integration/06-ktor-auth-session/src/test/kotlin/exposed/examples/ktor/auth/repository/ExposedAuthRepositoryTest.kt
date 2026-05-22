package exposed.examples.ktor.auth.repository

import exposed.examples.ktor.auth.persistence.AuthPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedAuthRepositoryTest {

    @Test
    fun `repository seeds users and persists session metadata`() = runTest {
        AuthPersistence.inMemory("repo_${Base58.randomString(8)}").use { persistence ->
            val repository = ExposedAuthRepository(persistence.database)

            val alice = repository.findUser("alice").shouldNotBeNull()
            alice.roles shouldContain "USER"

            val created = repository.createSession(alice.username)
            created.username shouldBeEqualTo "alice"
            val token = created.token.shouldNotBeNull()

            val sessions = repository.findSessions("alice")
            val listed = sessions.single { it.username == "alice" }
            listed.token.shouldBeNull()
            (listed.expiresAtEpochMs > listed.issuedAtEpochMs) shouldBeEqualTo true
            repository.findSessionByToken(token).shouldNotBeNull()
        }
    }
}
