package exposed.examples.ktor.auth.service

import exposed.examples.ktor.auth.persistence.AuthPersistence
import exposed.examples.ktor.auth.repository.ExposedAuthRepository
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthServiceTest {

    @Test
    fun `service authenticates valid credentials and rejects invalid credentials`() = runTest {
        AuthPersistence.inMemory("service_${Base58.randomString(8)}").use { persistence ->
            val service = AuthService(ExposedAuthRepository(persistence.database))

            val alice = service.authenticate("alice", "password").shouldNotBeNull()
            alice.roles shouldContain "USER"

            service.authenticate("alice", "wrong") shouldBeEqualTo null
            service.authenticate("missing", "password") shouldBeEqualTo null
        }
    }
}
