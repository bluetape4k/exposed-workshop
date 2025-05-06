package exposed.examples.cache.controller

import exposed.examples.cache.AbstractCacheStrategyTest
import exposed.examples.cache.domain.model.UserEventTable
import exposed.examples.cache.domain.model.newUserEventDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.tests.httpPost
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Duration

class UserEventControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractCacheStrategyTest() {

    companion object: KLogging()

    private fun getCountOfUserEvents(): Long = transaction {
        UserEventTable.selectAll().count()
    }

    @Test
    fun `insert user event`() {
        val prevCount = getCountOfUserEvents()

        val userEvent = newUserEventDTO()
        val response = client.httpPost("/user-events", userEvent)
            .expectBody<Boolean>()
            .returnResult().responseBody!!

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
    fun `bulk insert user events`() {
        val insertCount = 500
        val prevCount = getCountOfUserEvents()

        val userEvents = List(insertCount) { newUserEventDTO() }
        val response = client.httpPost("/user-events/bulk", userEvents)
            .expectBody<Boolean>()
            .returnResult().responseBody!!

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
