package exposed.examples.cache.controller

import exposed.examples.cache.AbstractCacheStrategyTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

class IndexControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractCacheStrategyTest() {

    @Test
    fun `call index`() {
        client.get()
            .uri("/")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("name").isNotEmpty()
    }
}
