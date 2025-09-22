package exposed.examples.cache.coroutines.controller

import exposed.examples.cache.coroutines.AbstractCacheStrategyTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class IndexControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel()

    @Test
    fun `call index`() = runSuspendIO {
        val buildProps = client.httpGet("/")
            .expectBody<String>()
            .returnResult().responseBody!!

        log.debug { "Build properties: $buildProps" }
        buildProps shouldContainAll listOf("Exposed", "Redisson")
    }
}
