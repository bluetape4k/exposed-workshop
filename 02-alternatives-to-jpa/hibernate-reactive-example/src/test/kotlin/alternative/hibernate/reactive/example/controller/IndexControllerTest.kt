package alternative.hibernate.reactive.example.controller

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

class IndexControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractHibernateReactiveTest() {

    companion object: KLoggingChannel()

    @Test
    fun `get root path`() = runSuspendIO {
        val response = client.get()
            .uri("/")
            .exchange()
            .expectStatus().isOk
            .returnResult<String>().responseBody
            .asFlow()
            .toList()
            .joinToString("")

        log.debug { "Response: $response" }
        response.shouldNotBeEmpty()
    }
}
