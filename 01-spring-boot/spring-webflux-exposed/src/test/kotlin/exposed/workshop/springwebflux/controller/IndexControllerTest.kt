package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import io.bluetape4k.spring.tests.httpGet
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class IndexControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractSpringWebfluxTest() {

    @Test
    fun `get index`() {
        client.httpGet("/")
            .expectStatus().isOk
            .expectBody<String>().returnResult().responseBody!!
            .shouldNotBeEmpty()
    }
}
