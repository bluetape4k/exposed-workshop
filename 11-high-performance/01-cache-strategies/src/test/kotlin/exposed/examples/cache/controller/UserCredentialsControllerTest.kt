package exposed.examples.cache.controller

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserCredentialsRecord
import exposed.examples.cache.domain.model.UserCredentialsTable
import exposed.examples.cache.domain.repository.UserCredentialsCacheRepository
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult
import java.time.Instant

class UserCredentialsControllerTest(
    @param:Autowired private val client: WebTestClient,
    @param:Autowired private val repository: UserCredentialsCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
    }

    private val idsInDB = mutableListOf<String>()
    private val idSize = 100

    @BeforeEach
    fun beforeEach() {
        repository.invalidateAll()
        idsInDB.clear()

        transaction {
            UserCredentialsTable.deleteAll()

            repeat(idSize) {
                idsInDB.add(insertUserCredentials())
            }
        }
    }

    private fun insertUserCredentials(): String {
        return UserCredentialsTable.insertAndGetId {
            it[UserCredentialsTable.username] = faker.credentials().username()
            it[UserCredentialsTable.email] = faker.internet().emailAddress()
            it[UserCredentialsTable.lastLoginAt] = Instant.now()
        }.value
    }

    @Test
    fun `findAll user credentials`() = runSuspendIO {

        val ucs = client
            .httpGet("/user-credentials")
            .expectStatus().is2xxSuccessful
            .expectBodyList<UserCredentialsRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        ucs shouldHaveSize idsInDB.size
    }

    @Test
    fun `find by id with read-through`() = runSuspendIO {
        idsInDB.forEach { id ->
            val uc = client
                .httpGet("/user-credentials/$id")
                .expectStatus().is2xxSuccessful
                .returnResult<UserCredentialsRecord>().responseBody
                .awaitSingle()

            log.debug { "UserCredentials[$id]: $uc" }
            uc.id shouldBeEqualTo id
        }
    }

    @Test
    fun `존재하지 않는 인증 ID로 조회하면 빈 응답을 반환한다`() = runSuspendIO {
        client
            .httpGet("/user-credentials/not-exists-id")
            .expectStatus().is2xxSuccessful
            .expectBody().isEmpty
    }

    @Test
    fun `복수의 ID로 UserCredentials를 Read-Through 방식으로 조회`() = runSuspendIO {
        val ids = idsInDB.shuffled().take(5)
        log.debug { "User credentials IDs to search: $ids" }
        val ucs = client
            .httpGet("/user-credentials/all?ids=${ids.joinToString(",")}")
            .expectStatus().is2xxSuccessful
            .expectBodyList<UserCredentialsRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()


        ucs shouldHaveSize ids.size
        ucs.map { it.id } shouldContainSame ids
    }

    @Test
    fun `존재하지 않는 인증 ID 목록으로 조회하면 빈 리스트를 반환한다`() = runSuspendIO {
        val ucs = client
            .httpGet("/user-credentials/all?ids=missing-1,missing-2")
            .expectStatus().is2xxSuccessful
            .expectBodyList<UserCredentialsRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        ucs.shouldBeEmpty()
    }

    @Test
    fun `invalidate specified cached user credentials`() = runSuspendIO {
        repository.getAll(idsInDB)

        val invalidatedIds = idsInDB.shuffled().take(3)
        val invalidatedCount = client
            .httpDelete("/user-credentials/invalidate?ids=${invalidatedIds.joinToString(",")}")
            .expectStatus().is2xxSuccessful
            .returnResult<Long>().responseBody
            .awaitSingle()

        log.debug { "invalidated count: $invalidatedCount" }
        invalidatedCount shouldBeEqualTo invalidatedIds.size.toLong()
    }
}
