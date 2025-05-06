package exposed.examples.cache.domain.repository


import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserTable
import io.bluetape4k.exposed.sql.statements.api.toExposedBlob
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.utility.Base58
import java.time.LocalDate
import kotlin.system.measureTimeMillis

class UserCacheRepositoryTest(
    @Autowired private val userRepository: UserCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLogging()

    private val userIdsInDB = mutableListOf<Long>()

    @BeforeEach
    fun beforeEach() {
        userRepository.invalidateAll()
        userIdsInDB.clear()

        transaction {
            UserTable.deleteAll()
            repeat(10) {
                userIdsInDB.add(insertUser())
            }
        }
        Thread.sleep(10)
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
    fun `Read Through 로 기존 DB정보를 캐시에서 읽어오기`() {
        transaction {
            val userId = userIdsInDB.random()
            log.debug { "User created. id=$userId" }

            val cachedUser = userRepository.get(userId)!!
            cachedUser.id shouldBeEqualTo userId

            val timeForDB = measureTimeMillis {
                userIdsInDB.forEach { id ->
                    userRepository.get(id).shouldNotBeNull()
                }
            }
            val timeForCache = measureTimeMillis {
                userIdsInDB.forEach { id ->
                    userRepository.get(id).shouldNotBeNull()
                }
            }
            log.debug { "Load Time. time for DB=$timeForDB, time for Cache=$timeForCache" }
            timeForCache shouldBeLessOrEqualTo timeForDB
        }
    }

    @Test
    fun `Read Through 로 복수의 User를 캐시에서 읽어오기`() {
        val userIdToSearch = userIdsInDB.shuffled().take(5)
        transaction {
            // DB에 있는 User를 검색
            val users = userRepository.getAll(userIdToSearch)
            users shouldHaveSize userIdToSearch.size
            users.forEach {
                log.debug { "Found user: $it" }
            }

            // 캐시에서 검색
            val users2 = userRepository.getAll(userIdToSearch)
            users2 shouldHaveSize userIdToSearch.size
        }
    }

    @Test
    fun `Read Through로 User를 검색한다`() {
        val users = transaction {
            userRepository.findAll()
        }
        users shouldHaveSize userIdsInDB.size
        users.forEach {
            log.debug { "Found user: $it" }
        }
    }

    @Test
    fun `Read Through 로 검색한 User가 없을 때에는 빈 리스트 반환`() {
        val userIdToSearch = listOf(-1L, -3L, -5L, -7L, -9L)
        val users = transaction {
            userRepository.findAll {
                UserTable.id inList userIdToSearch
            }
        }
        users.shouldBeEmpty()
    }

    @Test
    fun `Read Through 로 읽은 엔티티를 갱신하여 Write Through로 DB에 저장하기`() {
        transaction {
            val userId = userIdsInDB.random()

            val cachedUser = userRepository.get(userId)!!
            val updatedUser = cachedUser.copy(
                firstName = "updatedFirstName-${Base58.randomString(8)}",
                lastName = "updatedLastName-${Base58.randomString(8)}",
                address = "updatedAddress",
                zipcode = "updatedZipcode",
            ).also {
                it.avatar = faker.image().base64JPG().toByteArray()
            }
            userRepository.put(updatedUser)

            val userFromDB = userRepository.findFreshById(userId)
            userFromDB shouldBeEqualTo updatedUser.copy(updatedAt = userFromDB!!.updatedAt)
        }
    }
}
