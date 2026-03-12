package exposed.examples.cache.domain.repository

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserEventTable
import exposed.examples.cache.domain.model.newUserEventRecord
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

/** Write-Behind 캐시 전략을 사용해 대량의 UserEvent를 비동기로 DB에 저장하는 동작을 검증합니다. */
class UserEventCacheRepositoryTest(
    @param:Autowired private val repository: UserEventCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel()

    @BeforeEach
    fun setup() {
        repository.invalidateAll()

        transaction {
            UserEventTable.deleteAll()
        }
    }


    @Test
    fun `write behind 로 대량의 데이테를 추가한다`() {
        transaction {
            val totalCount = 10_000
            generateSequence { newUserEventRecord() }
                .take(totalCount)
                .chunked(100) { chunk ->
                    repository.putAll(chunk)
                }
                .toList()

            await
                .atMost(Duration.ofSeconds(10))
                .withPollInterval(Duration.ofMillis(50))
                .until {
                    transaction {
                        // 캐시뿐 아니라 Write behind로 DB에 저장될 때까지 대기합니다.
                        val savedEventCount = UserEventTable.selectAll().count()
                        log.debug { "Saved event count:$savedEventCount" }
                        savedEventCount >= totalCount.toLong()
                    }
                }

            UserEventTable.selectAll().count() shouldBeEqualTo totalCount.toLong()
        }
    }
}
