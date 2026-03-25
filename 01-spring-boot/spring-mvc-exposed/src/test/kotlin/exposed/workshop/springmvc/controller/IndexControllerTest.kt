package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.AbstractSpringMvcTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

/**
 * Spring MVC 환경에서 루트 경로(`/`) 엔드포인트를 검증하는 통합 테스트.
 *
 * [IndexController]가 빌드 정보를 JSON으로 반환하는지 확인합니다.
 */
class IndexControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractSpringMvcTest() {

    companion object: KLogging()

    @Test
    fun `get index returns build info`() = runSuspendIO {
        client.httpGet("/")
            .expectStatus().is2xxSuccessful
            .expectBody<String>()
            .returnResult().responseBody
            .apply { log.debug { "Index=$this" } }
            .shouldNotBeNull()
            .shouldNotBeEmpty()
    }
}
