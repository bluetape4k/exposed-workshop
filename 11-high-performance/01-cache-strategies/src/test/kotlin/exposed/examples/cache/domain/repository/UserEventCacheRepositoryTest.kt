package exposed.examples.cache.domain.repository

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserEventTable
import exposed.examples.cache.domain.model.newUserEventDTO
import io.bluetape4k.logging.coroutines.KLoggingChannel
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
            val totalCount = 1000
            generateSequence { newUserEventDTO() }
                .take(totalCount)
                .chunked(100) { chunk ->
                    repository.putAll(chunk)
                }
                .toList()

            await
                .atMost(Duration.ofSeconds(10))
                .withPollInterval(Duration.ofMillis(500))
                .until {
                    transaction { UserEventTable.selectAll().count() == totalCount.toLong() }
                }

            UserEventTable.selectAll().count() shouldBeEqualTo totalCount.toLong()
        }
    }
}
