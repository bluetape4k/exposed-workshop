package exposed.examples.cache.controller

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserEventTable
import exposed.examples.cache.domain.model.newUserEventDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import java.time.Duration

class UserEventControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel()

    private fun getCountOfUserEvents(): Long = transaction {
        UserEventTable.selectAll().count()
    }

    @Test
    fun `insert user event`() = runSuspendIO {
        val prevCount = getCountOfUserEvents()

        val userEvent = newUserEventDTO()
        val response = client
            .httpPost("/user-events", userEvent)
            .returnResult<Boolean>().responseBody
            .awaitSingle()

        response.shouldBeTrue()

        // 비동기로 처리되므로, await를 사용하여 결과를 기다림
        await.atMost(Duration.ofSeconds(4))
            .pollInterval(Duration.ofMillis(100))
            .until {
                getCountOfUserEvents() == prevCount + 1L
            }

        val currCount = getCountOfUserEvents()
        currCount shouldBeEqualTo prevCount + 1L
    }

    @Test
    fun `bulk insert user events`() = runSuspendIO {
        val insertCount = 1000
        val prevCount = getCountOfUserEvents()

        val userEvents = List(insertCount) { newUserEventDTO() }
        val response = client
            .httpPost("/user-events/bulk", userEvents)
            .returnResult<Boolean>().responseBody
            .awaitSingle()

        response.shouldBeTrue()

        // 비동기로 처리되므로, await를 사용하여 결과를 기다림
        await.atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .until {
                getCountOfUserEvents() == prevCount + insertCount
            }

        val currCount = getCountOfUserEvents()
        currCount shouldBeEqualTo prevCount + insertCount
    }
}
