package exposed.examples.cache.coroutines.domain.repository

import exposed.examples.cache.coroutines.AbstractCacheStrategyTest
import exposed.examples.cache.coroutines.domain.model.UserEventTable
import exposed.examples.cache.coroutines.domain.model.newUserEventDTO
import io.bluetape4k.junit5.awaitility.coUntil
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

class UserEventCacheRepositoryTest(
    @Autowired private val repository: UserEventCacheRepository,
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
    fun `write behind 로 대량의 데이테를 추가한다`() = runSuspendIO {
        newSuspendedTransaction {
            val totalCount = 1000

            flow {
                repeat(totalCount) {
                    emit(newUserEventDTO())
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
                .coUntil {
                    val countInDB = newSuspendedTransaction { UserEventTable.selectAll().count() }
                    log.debug { "countInDB: $countInDB" }
                    countInDB == totalCount.toLong()
                }

            UserEventTable.selectAll().count() shouldBeEqualTo totalCount.toLong()
        }
    }
}
