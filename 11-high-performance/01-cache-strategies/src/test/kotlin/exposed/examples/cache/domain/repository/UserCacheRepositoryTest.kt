package exposed.examples.cache.domain.repository


import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserTable
import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.utility.Base58
import java.time.LocalDate
import kotlin.system.measureTimeMillis

class UserCacheRepositoryTest(
    @param:Autowired private val repository: UserCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel()

    private val idsInDB = mutableListOf<Long>()

    @BeforeEach
    fun beforeEach() {
        repository.invalidateAll()
        idsInDB.clear()

        transaction {
            UserTable.deleteAll()
            repeat(10) {
                idsInDB.add(insertUser())
            }
        }
    }

    private fun insertUser(): Long {
        return UserTable.insertAndGetId {
            it[username] = faker.credentials().username()
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
            val userId = idsInDB.random()
            log.debug { "User created. id=$userId" }

            val cachedUser = repository.get(userId)!!
            cachedUser.id shouldBeEqualTo userId

            val timeForDB = measureTimeMillis {
                idsInDB.forEach { id ->
                    repository.get(id).shouldNotBeNull()
                }
            }
            val timeForCache = measureTimeMillis {
                idsInDB.forEach { id ->
                    repository.get(id).shouldNotBeNull()
                }
            }
            log.debug { "Load Time. time for DB=$timeForDB, time for Cache=$timeForCache" }
            timeForCache shouldBeLessOrEqualTo timeForDB
        }
    }

    @Test
    fun `Read Through 로 복수의 User를 캐시에서 읽어오기`() {
        val userIdToSearch = idsInDB.shuffled().take(5)
        transaction {
            // DB에 있는 User를 검색
            val users = repository.getAll(userIdToSearch)
            log.debug { "Loaded users from DB: ${users.size}" }
            users.forEach {
                log.debug { "Found user: $it" }
            }
            users shouldHaveSize userIdToSearch.size

            // 캐시에서 검색
            val users2 = repository.getAll(userIdToSearch)
            log.debug { "Loaded users from cache: ${users2.size}" }
            users2 shouldHaveSize userIdToSearch.size
        }
    }

    @Test
    fun `Read Through로 User를 검색한다`() {
        val users = transaction {
            repository.findAll()
        }
        users.forEach {
            log.debug { "Found user: $it" }
        }
        users shouldHaveSize idsInDB.size
    }

    @Test
    fun `Read Through 로 검색한 User가 없을 때에는 빈 리스트 반환`() {
        val userIdToSearch = listOf(-1L, -3L, -5L, -7L, -9L)
        val users = transaction {
            repository.findAll {
                UserTable.id inList userIdToSearch
            }
        }
        users.shouldBeEmpty()
    }

    @Test
    fun `Read Through 로 읽은 엔티티를 갱신하여 Write Through로 DB에 저장하기`() {
        transaction {
            val userId = idsInDB.random()

            log.debug { "Find user. userId: $userId" }
            val cachedUser = repository.get(userId)!!

            val updatedUser = cachedUser.copy(
                firstName = "updatedFirstName-${Base58.randomString(8)}",
                lastName = "updatedLastName-${Base58.randomString(8)}",
                address = "updatedAddress",
                zipcode = "updatedZipcode",
            ).also {
                it.avatar = faker.image().base64JPG().toByteArray()
            }
            //  Writr through 로 DB에 저장
            repository.put(updatedUser)

            // Write through로 DB에 저장되었는지, Cache가 아닌 DB에서 직접 읽어온다.
            val userFromDB = repository.findFreshById(userId)
            log.debug { "User from DB: $userFromDB" }
            userFromDB shouldBeEqualTo updatedUser.copy(updatedAt = userFromDB!!.updatedAt)
        }
    }
}
