package exposed.examples.cache.controller

import exposed.examples.cache.AbstractCacheStrategyTest
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
): AbstractCacheStrategyTest() {

    @Test
    fun `call index`() = runSuspendIO {
        client.httpGet("/")
            .returnResult<String>().responseBody
            .awaitFirstOrNull()
            .shouldNotBeNull()
            .shouldNotBeEmpty()
    }
}
