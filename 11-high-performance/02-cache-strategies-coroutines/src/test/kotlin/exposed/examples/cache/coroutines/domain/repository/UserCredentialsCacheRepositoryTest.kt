package exposed.examples.cache.coroutines.domain.repository

import exposed.examples.cache.coroutines.AbstractCacheStrategyTest
import exposed.examples.cache.coroutines.domain.model.UserCredentialsTable
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/** 코루틴 기반 Read-Through 캐시 전략을 적용한 `UserCredentialsCacheRepository`의 조회 및 캐시 무효화를 코루틴 환경에서 검증합니다. */
@Suppress("DEPRECATION")
class UserCredentialsCacheRepositoryTest(
    @param:Autowired private val repository: UserCredentialsCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel()

    private val idsInDB = CopyOnWriteArrayList<String>()
    private val idSize = 100

    @BeforeEach
    fun setup() {
        runBlocking {
            repository.clear()
            idsInDB.clear()

            newSuspendedTransaction {
                UserCredentialsTable.deleteAll()

                repeat(idSize) {
                    idsInDB.add(insertUserCredentials())
                }
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
    fun `Read Through 로 기존 DB정보를 캐시에서 읽어오기`() = runSuspendIO {
        newSuspendedTransaction(readOnly = true) {
            idsInDB.forEach { ucId ->
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
    fun `Read Through 로 검색해서 가져오기`() = runSuspendIO {
        newSuspendedTransaction(readOnly = true) {
            val userCredentialsFromCache = repository.findAll {
                UserCredentialsTable.id inList idsInDB
            }
            userCredentialsFromCache shouldHaveSize idsInDB.size
        }
    }

    @Test
    fun `Read Through 로 모든 ID 가져오기`() = runSuspendIO {
        newSuspendedTransaction(readOnly = true) {
            val userCredentialsFromCache = repository.getAll(idsInDB).map { it.value }
            userCredentialsFromCache shouldHaveSize idsInDB.size
        }
    }

    @Test
    fun `존재하지 않는 인증 ID 조회 시 null을 반환한다`() = runSuspendIO {
        val uc = repository.get("missing-user-credentials-id")
        uc.shouldBeNull()
    }

    @Test
    fun `캐시 무효화 후 재조회 시 DB에서 다시 읽어온다`() = runSuspendIO {
        newSuspendedTransaction(readOnly = true) {
            val ucId = idsInDB.random()

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
