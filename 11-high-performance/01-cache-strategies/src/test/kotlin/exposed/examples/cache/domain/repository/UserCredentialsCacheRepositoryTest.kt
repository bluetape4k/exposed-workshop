package exposed.examples.cache.domain.repository

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserCredentialsTable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
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
import java.util.UUID

/** Read-Through 캐시 전략을 적용한 `UserCredentialsCacheRepository`의 조회 및 캐시 무효화를 검증합니다. */
class UserCredentialsCacheRepositoryTest(
    @param:Autowired private val repository: UserCredentialsCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel()

    private val userCredentialsIdsInDB = mutableListOf<String>()

    @BeforeEach
    fun setup() {
        repository.clear()
        userCredentialsIdsInDB.clear()

        transaction(database) {
            UserCredentialsTable.deleteAll()

            repeat(10) {
                userCredentialsIdsInDB.add(insertUserCredentials())
            }
        }
    }

    private fun insertUserCredentials(): String {
        return UserCredentialsTable.insertAndGetId {
            val username = UUID.randomUUID().toString()
            it[UserCredentialsTable.username] = username
            it[UserCredentialsTable.email] = "${username.take(24)}@t.dev"
            it[UserCredentialsTable.lastLoginAt] = Instant.now()
        }.value
    }

    @Test
    fun `Read Through 로 기존 DB정보를 캐시에서 읽어오기`() {
        transaction(database) {
            userCredentialsIdsInDB.forEach { ucId ->
                log.debug { "Get user credentials. id: $ucId" }
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
        transaction(database) {
            val userCredentialsFromCache = repository.findAll {
                UserCredentialsTable.id inList userCredentialsIdsInDB
            }
            userCredentialsFromCache.forEach { uc ->
                log.debug { "Founded user credentials: $uc" }
            }
            userCredentialsFromCache shouldHaveSize userCredentialsIdsInDB.size
            userCredentialsFromCache.map { it.id } shouldContainSame userCredentialsIdsInDB
        }
    }

    @Test
    fun `Read Through 로 모든 ID 가져오기`() {
        transaction(database) {
            val userCredentialsFromCache = repository.getAll(userCredentialsIdsInDB).map { it.value }
            userCredentialsFromCache shouldHaveSize userCredentialsIdsInDB.size
            userCredentialsFromCache.map { it.id } shouldContainSame userCredentialsIdsInDB
        }
    }

    @Test
    fun `존재하지 않는 인증 ID 조회 시 null을 반환한다`() {
        transaction(database) {
            repository.get("missing-user-credentials-id").shouldBeNull()
        }
    }

    @Test
    fun `캐시 무효화 후 재조회 시 DB에서 다시 읽어온다`() {
        transaction(database) {
            val ucId = userCredentialsIdsInDB.random()

            // 캐시에 로드
            repository.get(ucId).shouldNotBeNull()

            // 캐시 무효화
            repository.invalidate(ucId)

            // 재조회 시 DB에서 다시 읽어온다
            val reloaded = repository.get(ucId)
            reloaded.shouldNotBeNull()
            reloaded.id shouldBeEqualTo ucId
        }
    }
}
