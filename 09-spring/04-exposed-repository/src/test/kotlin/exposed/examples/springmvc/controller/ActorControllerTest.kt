package exposed.examples.springmvc.controller

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.dtos.ActorDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

class ActorControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractExposedRepositoryTest() {

    companion object: KLogging() {
        private fun newActor(): ActorDTO = ActorDTO(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday().toString()
        )
    }

    @Test
    fun `get actor by id`() {
        val id = 1L

        val actor = client
            .get()
            .uri("/actors/$id")
            .exchange()
            .expectStatus().isOk
            .expectBody<ActorDTO>().returnResult().responseBody

        log.debug { "actor=$actor" }

        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo id
    }

    @Test
    fun `find actors by lastName`() {
        val lastName = "Depp"

        val actors = client
            .get()
            .uri("/actors?lastName=$lastName")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<ActorDTO>()
            .returnResult().responseBody

        log.debug { "actors=$actors" }
        actors.shouldNotBeNull() shouldHaveSize 1
    }

    @Test
    fun `find actors by firstName`() {
        val firstName = "Angelina"

        val angelinas = client
            .get()
            .uri("/actors?firstName=$firstName")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<ActorDTO>()
            .returnResult().responseBody

        log.debug { "angelinas=$angelinas" }
        angelinas.shouldNotBeNull() shouldHaveSize 2
    }

    @Test
    fun `create actor`() {
        val actor = newActor()

        val newActor = client
            .post()
            .uri("/actors")
            .bodyValue(actor)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<ActorDTO>()
            .returnResult().responseBody!!

        newActor shouldBeEqualTo actor.copy(id = newActor.id)
    }

    @Test
    fun `delete actor`() {
        val actor = newActor()

        val newActor = client
            .post()
            .uri("/actors")
            .bodyValue(actor)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<ActorDTO>()
            .returnResult().responseBody!!

        val deletedCount = client
            .delete()
            .uri("/actors/${newActor.id}")
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Int>()
            .returnResult().responseBody!!

        deletedCount shouldBeEqualTo 1
    }
}
