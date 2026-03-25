package exposed.examples.cache.coroutines.domain.repository

import exposed.examples.cache.coroutines.AbstractCacheStrategyTest
import exposed.examples.cache.coroutines.domain.model.UserEventTable
import exposed.examples.cache.coroutines.domain.model.newUserEventRecord
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

/** 코루틴 기반 Write-Behind 캐시 전략을 사용해 대량의 UserEvent를 비동기로 DB에 저장하는 동작을 코루틴 환경에서 검증합니다. */
@Suppress("DEPRECATION")
class UserEventCacheRepositoryTest(
    @param:Autowired private val repository: UserEventCacheRepository,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel()

    @BeforeEach
    fun setup() {
        runBlocking {
            repository.invalidateAll()

            newSuspendedTransaction {
                UserEventTable.deleteAll()
            }
        }
    }

    @Test
    fun `write behind 로 단일 이벤트를 추가한다`() = runSuspendIO {
        newSuspendedTransaction {
            val event = newUserEventRecord()
            repository.put(event)

            await
                .atMost(Duration.ofSeconds(10))
                .withPollInterval(Duration.ofMillis(500))
                .untilSuspending {
                    val count = newSuspendedTransaction { UserEventTable.selectAll().count() }
                    log.debug { "countInDB: $count" }
                    count >= 1L
                }

            UserEventTable.selectAll().count() shouldBeEqualTo 1L
        }
    }

    @Test
    fun `write behind 로 대량의 데이테를 추가한다`() = runSuspendIO {
        newSuspendedTransaction {
            val totalCount = 1000

            flow {
                repeat(totalCount) {
                    emit(newUserEventRecord())
                }
            }
                .chunked(100)
                .collect { chunk ->
                    log.debug { "put all ${chunk.size} items" }
                    repository.putAll(chunk)
                }

            // Write-Behind 이므로, DB에 반영되기까지 시간이 걸린다.
            await
                .atMost(Duration.ofSeconds(10))
                .withPollInterval(Duration.ofMillis(500))
                .untilSuspending {
                    val countInDB = newSuspendedTransaction { UserEventTable.selectAll().count() }
                    log.debug { "countInDB: $countInDB" }
                    countInDB == totalCount.toLong()
                }

            UserEventTable.selectAll().count() shouldBeEqualTo totalCount.toLong()
        }
    }
}
