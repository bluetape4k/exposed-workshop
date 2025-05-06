package exposed.examples.cache.controller

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserCredentialsDTO
import exposed.examples.cache.domain.model.UserCredentialsTable
import exposed.examples.cache.domain.repository.UserCredentialsCacheRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import java.time.Instant

class UserCredentialsControllerTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val repository: UserCredentialsCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val idsInDB = mutableListOf<String>()

    @BeforeEach
    fun beforeEach() {
        repository.invalidateAll()
        idsInDB.clear()

        transaction {
            UserCredentialsTable.deleteAll()

            repeat(10) {
                idsInDB.add(insertUserCredentials())
            }
        }
    }

    private fun insertUserCredentials(): String {
        return UserCredentialsTable.insertAndGetId {
            it[UserCredentialsTable.username] = faker.internet().username()
            it[UserCredentialsTable.email] = faker.internet().emailAddress()
            it[UserCredentialsTable.lastLoginAt] = Instant.now()
        }.value
    }

    @Test
    fun `findAll user credentials`() {
        transaction {
            val ucs = client
                .httpGet("/user-credentials")
                .expectBodyList<UserCredentialsDTO>()
                .returnResult().responseBody!!

            ucs shouldHaveSize idsInDB.size
        }
    }

    @Test
    fun `find by id with read-through`() {
        idsInDB.forEach { id ->
            val uc = client
                .httpGet("/user-credentials/$id")
                .expectBody<UserCredentialsDTO>()
                .returnResult().responseBody!!
            uc.id shouldBeEqualTo id
        }
    }

    @Test
    fun `복수의 ID로 UserCredentials를 Read-Through 방식으로 조회`() {
        val ids = idsInDB.shuffled().take(5)
        log.debug { "User credentials IDs to search: $ids" }
        val ucs = client
            .httpGet("/user-credentials/all?ids=${ids.joinToString(",")}")
            .expectBodyList<UserCredentialsDTO>()
            .returnResult().responseBody!!

        ucs shouldHaveSize ids.size
        ucs.map { it.id } shouldContainSame ids
    }

    @Test
    fun `invalidate specified cached user credentials`() {
        repository.getAll(idsInDB)

        val invalidatedIds = idsInDB.shuffled().take(3)
        val invalidateCount = client
            .httpDelete("/user-credentials/invalidate?ids=${invalidatedIds.joinToString(",")}")
            .expectBody<Long>()
            .returnResult().responseBody!!

        invalidateCount shouldBeEqualTo invalidatedIds.size.toLong()
    }
}
