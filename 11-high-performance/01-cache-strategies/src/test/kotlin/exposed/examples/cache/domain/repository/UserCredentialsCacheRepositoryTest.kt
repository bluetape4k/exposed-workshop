package exposed.examples.cache.domain.repository

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserCredentialsTable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class UserCredentialsCacheRepositoryTest(
    @Autowired private val repository: UserCredentialsCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel()

    private val userCredentialsIdsInDB = mutableListOf<String>()

    @BeforeEach
    fun setup() {
        repository.invalidateAll()
        userCredentialsIdsInDB.clear()

        transaction {
            UserCredentialsTable.deleteAll()

            repeat(10) {
                userCredentialsIdsInDB.add(insertUserCredentials())
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
    fun `Read Through 로 기존 DB정보를 캐시에서 읽어오기`() {
        transaction {
            userCredentialsIdsInDB.forEach { ucId ->
                val userCredentialsFromCache = repository.get(ucId)
                log.debug { "Loaded user credentials from cache. id=$ucId, $userCredentialsFromCache" }
                userCredentialsFromCache.shouldNotBeNull()
                userCredentialsFromCache.id shouldBeEqualTo ucId
                userCredentialsFromCache.username shouldBeEqualTo UserCredentialsTable.selectAll()
                    .where { UserCredentialsTable.id eq ucId }
                    .single()[UserCredentialsTable.username]
            }
        }
    }

    @Test
    fun `Read Through 로 검색해서 가져오기`() {
        transaction {
            val userCredentialsFromCache = repository.findAll {
                UserCredentialsTable.id inList userCredentialsIdsInDB
            }
            userCredentialsFromCache shouldHaveSize userCredentialsIdsInDB.size
        }
    }

    @Test
    fun `Read Through 로 모든 ID 가져오기`() {
        transaction {
            val userCredentialsFromCache = repository.getAll(userCredentialsIdsInDB)
            userCredentialsFromCache shouldHaveSize userCredentialsIdsInDB.size
        }
    }
}
