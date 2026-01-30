package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class IndexControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractSpringWebfluxTest() {

    companion object: KLoggingChannel()

    @Test
    fun `get index`() = runSuspendIO {
        client.httpGet("/")
            .expectStatus().is2xxSuccessful
            .expectBody<String>()
            .returnResult().responseBody
            .apply {
                log.debug { "Index=$this" }
            }
            .shouldNotBeNull()
            .shouldNotBeEmpty()

    }
}
