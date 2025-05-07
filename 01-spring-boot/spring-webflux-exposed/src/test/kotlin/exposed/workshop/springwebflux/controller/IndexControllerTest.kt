package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.spring.tests.httpGet
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

class IndexControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractSpringWebfluxTest() {

    @Test
    fun `get index`() = runSuspendIO {
        client.httpGet("/")
            .expectStatus().isOk
            .returnResult<String>().responseBody
            .awaitFirstOrNull()
            .shouldNotBeNull()
            .shouldNotBeEmpty()
    }
}
