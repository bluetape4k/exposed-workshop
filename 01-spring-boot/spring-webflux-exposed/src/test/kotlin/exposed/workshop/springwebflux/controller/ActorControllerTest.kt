package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import exposed.workshop.springwebflux.domain.model.ActorRecord
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

/**
 * Spring WebFlux + Coroutine 환경에서 배우(Actor) REST API 엔드포인트를 검증하는 통합 테스트 클래스.
 *
 * [WebTestClient]를 이용해 `/actors` 경로의 GET/POST/DELETE 요청을 suspend 방식으로 호출하며,
 * 조회·생성·삭제·잘못된 파라미터 처리 시나리오를 비동기 흐름에서 검증한다.
 */
class ActorControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractSpringWebfluxTest() {

    companion object: KLoggingChannel() {
        private fun newActorRecord(): ActorRecord = ActorRecord(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday().toString()
        )
    }

    @Test
    fun `get actor by id`() = runSuspendIO {
        val id = 1L

        val actor = client
            .httpGet("/actors/$id")
            .expectStatus().is2xxSuccessful
            .returnResult<ActorRecord>().responseBody
            .awaitSingle()

        log.debug { "actor=$actor" }

        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo id
    }

    @Test
    fun `find actors by lastName`() = runSuspendIO {
        val lastName = "Depp"

        val depp = client
            .httpGet("/actors?lastName=$lastName")
            .expectStatus().is2xxSuccessful
            .expectBodyList<ActorRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        log.debug { "actors=$depp" }
        depp shouldHaveSize 1
    }

    @Test
    fun `find actors by firstName`() = runSuspendIO {
        val firstName = "Angelina"

        val angelinas = client
            .httpGet("/actors?firstName=$firstName")
            .expectStatus().is2xxSuccessful
            .expectBodyList<ActorRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        log.debug { "actors=$angelinas" }
        angelinas.shouldNotBeNull() shouldHaveSize 2
    }

    @Test
    fun `create new actor`() = runSuspendIO {
        val actor = newActorRecord()

        val newActor = client
            .httpPost("/actors", actor)
            .expectStatus().is2xxSuccessful
            .returnResult<ActorRecord>().responseBody
            .awaitSingle()

        log.debug { "newActor=$newActor" }
        newActor shouldBeEqualTo actor.copy(id = newActor.id)
    }

    /** 배우를 생성한 후 삭제 요청이 성공하고 삭제 건수 1이 반환되는지 suspend 흐름에서 검증한다. */
    @Test
    fun `delete actor`() = runSuspendIO {
        val actor = newActorRecord()

        val newActor = client
            .httpPost("/actors", actor)
            .expectStatus().is2xxSuccessful
            .returnResult<ActorRecord>().responseBody
            .awaitSingle()

        log.debug { "newActor=$newActor" }

        val deletedCount = client
            .httpDelete("/actors/${newActor.id}")
            .expectStatus().is2xxSuccessful
            .returnResult<Int>().responseBody
            .awaitSingle()

        log.debug { "deletedCount=$deletedCount" }
        deletedCount shouldBeEqualTo 1
    }

    /** 유효하지 않은 birthday 파라미터가 전달되어도 오류 없이 전체 목록을 반환하는지 검증한다. */
    @Test
    fun `search actors ignores invalid birthday parameter`() = runSuspendIO {
        val actors = client
            .httpGet("/actors?birthday=not-a-date")
            .expectStatus().is2xxSuccessful
            .expectBodyList<ActorRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        actors.shouldNotBeEmpty()
    }
}
