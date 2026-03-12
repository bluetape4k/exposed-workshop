package exposed.examples.cache.coroutines.controller

import exposed.examples.cache.coroutines.AbstractCacheStrategyTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

/** `GET /` 인덱스 엔드포인트가 코루틴 환경에서 정상 응답을 반환함을 검증합니다. */
class IndexControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractCacheStrategyTest() {

    companion object: KLoggingChannel()

    @Test
    fun `call index`() = runSuspendIO {
        client
            .httpGet("/")
            .returnResult<String>().responseBody
            .awaitSingle()
            .apply {
                log.debug { "index response: $this" }
            }
            .shouldNotBeEmpty()
    }
}
