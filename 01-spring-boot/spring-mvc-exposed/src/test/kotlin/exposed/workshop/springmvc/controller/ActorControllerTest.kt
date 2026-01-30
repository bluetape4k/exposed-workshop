package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.AbstractSpringMvcTest
import exposed.workshop.springmvc.domain.ActorDTO
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
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

class ActorControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractSpringMvcTest() {

    companion object: KLogging() {
        private fun newActor(): ActorDTO = ActorDTO(
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
            .returnResult<ActorDTO>().responseBody
            .awaitSingle()
            .shouldNotBeNull()

        log.debug { "actor=$actor" }

        actor.id shouldBeEqualTo id
    }

    @Test
    fun `find actors by lastName`() = runSuspendIO {
        val lastName = "Depp"

        val actors = client
            .httpGet("/actors?lastName=$lastName")
            .expectStatus().is2xxSuccessful
            .expectBodyList<ActorDTO>()
            .returnResult().responseBody
            .shouldNotBeNull()

        log.debug { "actors=$actors" }
        actors shouldHaveSize 1
    }

    @Test
    fun `find actors by firstName`() = runSuspendIO {
        val firstName = "Angelina"

        val angelinas = client
            .httpGet("/actors?firstName=$firstName")
            .expectStatus().is2xxSuccessful
            .expectBodyList<ActorDTO>()
            .returnResult().responseBody
            .shouldNotBeNull()

        log.debug { "angelinas=$angelinas" }
        angelinas shouldHaveSize 2
    }

    @Test
    fun `create actor`() = runSuspendIO {
        val actor = newActor()

        val newActor = client
            .httpPost("/actors", actor)
            .expectStatus().is2xxSuccessful
            .returnResult<ActorDTO>().responseBody
            .awaitSingle()

        newActor shouldBeEqualTo actor.copy(id = newActor.id)
    }

    @Test
    fun `delete actor`() = runSuspendIO {
        val actor = newActor()

        val newActor = client
            .httpPost("/actors", actor)
            .expectStatus().is2xxSuccessful
            .returnResult<ActorDTO>().responseBody
            .awaitSingle()

        val deletedCount = client
            .httpDelete("/actors/${newActor.id}")
            .expectStatus().is2xxSuccessful
            .returnResult<Int>().responseBody
            .awaitSingle()

        deletedCount shouldBeEqualTo 1
    }
}
