package exposed.examples.ktor.auth.repository

import exposed.examples.ktor.auth.persistence.AuthPersistence
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.bluetape4k.tink.digest.TinkDigesters
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedAuthRepositoryTest {

    @Test
    fun `tink SHA-256 hex preserves UTF-8 lowercase 64-character contract`() {
        TinkDigesters.SHA256.digestHex("") shouldBeEqualTo
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        TinkDigesters.SHA256.digestHex("한글🔐") shouldBeEqualTo
            "4bf6e938ebc1c053c46bb45c34671c82ba63137812782261f5c709cf6766ee11"
        TinkDigesters.SHA256.digestHex("leading-zero-14") shouldBeEqualTo
            "0254752e819d44696c55ce451caf98ca6cd6f71254c516ab05910f79815df755"
    }

    @Test
    fun `repository seeds users and persists session metadata`() = runTest {
        AuthPersistence.inMemory("repo_${Base58.randomString(8)}").use { persistence ->
            val repository = ExposedAuthRepository(persistence.database)

            val alice = repository.findUser("alice").shouldNotBeNull()
            alice.roles shouldContain "USER"

            val created = repository.createSession(alice.username)
            created.username shouldBeEqualTo "alice"
            val token = created.token.shouldNotBeNull()

            val expectedHash = TinkDigesters.SHA256.digestHex(token)
            val storedHash = transaction(persistence.database) {
                AuthSessionsProbe.selectAll()
                    .where { AuthSessionsProbe.tokenHash eq expectedHash }
                    .single()[AuthSessionsProbe.tokenHash]
            }
            storedHash shouldBeEqualTo expectedHash
            storedHash.length shouldBeEqualTo 64
            storedHash shouldBeEqualTo storedHash.lowercase()

            val sessions = repository.findSessions("alice")
            val listed = sessions.single { it.username == "alice" }
            listed.token.shouldBeNull()
            (listed.expiresAtEpochMs > listed.issuedAtEpochMs) shouldBeEqualTo true
            repository.findSessionByToken(token).shouldNotBeNull()
        }
    }
}

private object AuthSessionsProbe : Table("ktor_auth_sessions") {
    val tokenHash = varchar("token_hash", 64)
}
