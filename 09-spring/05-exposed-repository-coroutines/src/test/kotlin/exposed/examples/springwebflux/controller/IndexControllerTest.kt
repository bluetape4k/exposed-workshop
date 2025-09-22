package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

class IndexControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel()

    @Test
    fun `get index`() = runSuspendIO {
        client.httpGet("/")
            .expectStatus().isOk
            .returnResult<String>().responseBody
            .asFlow()
            .toList()
            .shouldNotBeEmpty()
            .let {
                log.debug { "index response=${it.joinToString("\n")}" }
            }
    }
}
