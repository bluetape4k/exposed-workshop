package alternative.hibernate.reactive.example.controller

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class IndexControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractHibernateReactiveTest() {

    companion object: KLogging()

    @Test
    fun `get root path`() {
        val response = client.get()
            .uri("/")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>().returnResult().responseBody

        log.debug { "Response: $response" }

    }
}
