package exposed.examples.cache.controller

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserDTO
import exposed.examples.cache.domain.model.UserTable
import exposed.examples.cache.domain.model.newUserDTO
import exposed.examples.cache.domain.repository.UserCacheRepository
import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
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
import org.springframework.test.web.reactive.server.returnResult
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

class UserControllerTest(
    @param:Autowired private val client: WebTestClient,
    @param:Autowired private val repository: UserCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
    }

    private val userIdsInDB = mutableListOf<Long>()
    private val lastUserId = AtomicLong(0L)

    @BeforeEach
    fun beforeEach() {
        repository.invalidateAll()
        userIdsInDB.clear()

        transaction {
            UserTable.deleteAll()
            repeat(10) {
                userIdsInDB.add(insertUser())
            }
        }
    }

    private fun insertUser(): Long {
        return UserTable.insertAndGetId {
            it[username] = faker.internet().username()
            it[firstName] = faker.name().firstName()
            it[lastName] = faker.name().lastName()
            it[address] = faker.address().fullAddress()
            it[zipcode] = faker.address().zipCode()
            it[birthDate] = LocalDate.now()
            it[avatar] = faker.image().base64JPG().toByteArray().toExposedBlob()
        }.value
    }

    @Test
    fun `모든 사용자를 조회`() = runSuspendIO {
        val users = client
            .httpGet("/users")
            .returnResult<UserDTO>().responseBody
            .asFlow()
            .toList()

        users.shouldNotBeNull() shouldHaveSize userIdsInDB.size
    }

    @Test
    fun `User ID로 User를 Read-Through 방식으로 조회`() = runSuspendIO {
        userIdsInDB.forEach { userId ->
            val user = client
                .httpGet("/users/$userId")
                .returnResult<UserDTO>().responseBody
                .awaitSingle()

            user.id shouldBeEqualTo userId
        }
    }

    @Test
    fun `복수의 User ID로 User를 Read-Through 방식으로 조회`() = runSuspendIO {
        val userIds = userIdsInDB.shuffled().take(5)
        log.debug { "User IDs to search: $userIds" }

        val users = client
            .httpGet("/users/all?ids=${userIds.joinToString(",")}")
            .returnResult<UserDTO>().responseBody
            .asFlow()
            .toList()

        users shouldHaveSize userIds.size
        users.map { it.id } shouldContainSame userIds
    }

    @Test
    fun `새로운 User를 write through 로 저장하기`() = runSuspendIO {
        val userDTO = newUserDTO(kotlin.random.Random.nextLong(1000L, 9999L))
        val user = client
            .httpPost("/users", userDTO)
            .returnResult<UserDTO>().responseBody
            .awaitSingle()

        user.id shouldBeEqualTo userDTO.id
    }

    @Test
    fun `invalidate specified id cached user`() = runSuspendIO {
        repository.getAll(userIdsInDB)

        val invalidatedId = userIdsInDB.shuffled().take(3)
        val invalidedCount = client
            .httpDelete("/users/invalidate?ids=${invalidatedId.joinToString(",")}")
            .returnResult<Long>().responseBody
            .awaitSingle()

        invalidedCount shouldBeEqualTo invalidatedId.size.toLong()
    }
}
