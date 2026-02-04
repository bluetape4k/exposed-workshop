package exposed.examples.springmvc.controller

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.model.ActorRecord
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

class ActorControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractExposedRepositoryTest() {

    companion object: KLogging() {
        private fun newActor(): ActorRecord = ActorRecord(
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

        val actors = client
            .httpGet("/actors?lastName=$lastName")
            .expectStatus().is2xxSuccessful
            .expectBodyList<ActorRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        log.debug { "actors=$actors" }
        actors.shouldNotBeNull() shouldHaveSize 1
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

        log.debug { "angelinas=$angelinas" }
        angelinas.shouldNotBeNull() shouldHaveSize 2
    }

    @Test
    fun `create actor`() = runSuspendIO {
        val actor = newActor()

        val newActor = client
            .httpPost("/actors", actor)
            .expectStatus().is2xxSuccessful
            .expectBody<ActorRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        newActor shouldBeEqualTo actor.copy(id = newActor.id)
    }

    @Test
    fun `delete actor`() = runSuspendIO {
        val actor = newActor()

        val newActor = client
            .httpPost("/actors", actor)
            .expectStatus().is2xxSuccessful
            .expectBody<ActorRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        val deletedCount = client
            .httpDelete("/actors/${newActor.id}")
            .expectStatus().is2xxSuccessful
            .expectBody<Int>()
            .returnResult().responseBody
            .shouldNotBeNull()

        deletedCount shouldBeEqualTo 1
    }
}
