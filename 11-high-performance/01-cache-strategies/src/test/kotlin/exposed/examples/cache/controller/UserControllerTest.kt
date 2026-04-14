package exposed.examples.cache.controller

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserRecord
import exposed.examples.cache.domain.model.UserTable
import exposed.examples.cache.domain.model.newUserRecord
import exposed.examples.cache.domain.repository.UserCacheRepository
import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
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
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

/** Read-Through / Write-Through 캐시 전략을 적용한 User REST API의 CRUD 동작을 검증합니다. */
class UserControllerTest(
    @param:Autowired private val client: WebTestClient,
    @param:Autowired private val repository: UserCacheRepository,
) : AbstractCacheStrategyTest() {
    companion object : KLoggingChannel()

    private val userIdsInDB = CopyOnWriteArrayList<Long>()

    @BeforeEach
    fun beforeEach() {
        repository.clear()
        userIdsInDB.clear()

        transaction {
            UserTable.deleteAll()
            repeat(10) {
                userIdsInDB.add(insertUser())
            }
        }
    }

    private fun insertUser(): Long =
        UserTable
            .insertAndGetId {
                it[username] = faker.credentials().username()
                it[firstName] = faker.name().firstName()
                it[lastName] = faker.name().lastName()
                it[address] = faker.address().fullAddress()
                it[zipcode] = faker.address().zipCode()
                it[birthDate] = LocalDate.now()
                it[avatar] =
                    faker
                        .image()
                        .base64JPG()
                        .toByteArray()
                        .toExposedBlob()
            }.value

    @Test
    fun `모든 사용자를 조회`() =
        runSuspendIO {
            val users =
                client
                    .httpGet("/users")
                    .expectStatus()
                    .is2xxSuccessful
                    .expectBodyList<UserRecord>()
                    .returnResult()
                    .responseBody
                    .shouldNotBeNull()

            users shouldHaveSize userIdsInDB.size
        }

    @Test
    fun `User ID로 User를 Read-Through 방식으로 조회`() =
        runSuspendIO {
            userIdsInDB.forEach { userId ->
                val user =
                    client
                        .httpGet("/users/$userId")
                        .expectStatus()
                        .is2xxSuccessful
                        .returnResult<UserRecord>()
                        .responseBody
                        .awaitSingle()

                log.debug { "User[$userId]: $user" }
                user.id shouldBeEqualTo userId
            }
        }

    @Test
    fun `존재하지 않는 User ID로 조회하면 빈 응답을 반환한다`() =
        runSuspendIO {
            client
                .httpGet("/users/-999999")
                .expectStatus()
                .is2xxSuccessful
                .expectBody()
                .isEmpty
        }

    @Test
    fun `복수의 User ID로 User를 Read-Through 방식으로 조회`() =
        runSuspendIO {
            val userIds = userIdsInDB.shuffled().take(5)
            log.debug { "User IDs to search: $userIds" }

            val users =
                client
                    .httpGet("/users/all?ids=${userIds.joinToString(",")}")
                    .expectStatus()
                    .is2xxSuccessful
                    .expectBodyList<UserRecord>()
                    .returnResult()
                    .responseBody
                    .shouldNotBeNull()

            users shouldHaveSize userIds.size
            users.map { it.id } shouldContainSame userIds
        }

    @Test
    fun `존재하지 않는 User ID 목록으로 조회하면 빈 리스트를 반환한다`() =
        runSuspendIO {
            val users =
                client
                    .httpGet("/users/all?ids=-1,-2,-3")
                    .expectStatus()
                    .is2xxSuccessful
                    .expectBodyList<UserRecord>()
                    .returnResult()
                    .responseBody
                    .shouldNotBeNull()

            users.shouldBeEmpty()
        }

    @Test
    fun `새로운 User를 write through 로 저장하기`() =
        runSuspendIO {
            val newUser = newUserRecord(Random.nextLong(1000L, 9999L))
            val user =
                client
                    .httpPost("/users", newUser)
                    .expectStatus()
                    .is2xxSuccessful
                    .returnResult<UserRecord>()
                    .responseBody
                    .awaitSingle()

            log.debug { "Created user: $user" }
            user.id shouldBeEqualTo newUser.id
        }

    @Test
    fun `invalidate specified id cached user`() =
        runSuspendIO {
            repository.getAll(userIdsInDB)

            val invalidatedId = userIdsInDB.shuffled().take(3)
            val invalidatedCount =
                client
                    .httpDelete("/users/invalidate?ids=${invalidatedId.joinToString(",")}")
                    .expectStatus()
                    .is2xxSuccessful
                    .returnResult<Long>()
                    .responseBody
                    .awaitSingle()

            log.debug { "invalidated count: $invalidatedCount" }
            invalidatedCount shouldBeEqualTo invalidatedId.size.toLong()
        }
}
