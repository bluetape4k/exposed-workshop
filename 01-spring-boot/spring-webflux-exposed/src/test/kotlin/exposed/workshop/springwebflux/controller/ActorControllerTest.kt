package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import exposed.workshop.springwebflux.domain.ActorDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
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
): AbstractSpringWebfluxTest() {

    companion object: KLogging() {
        private fun newActorDTO(): ActorDTO = ActorDTO(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday().toString()
        )
    }

    @Test
    fun `get actor by id`() {
        val id = 1L

        val actor = client
            .httpGet("/actors/$id")
            .expectBody<ActorDTO>()
            .returnResult().responseBody

        log.debug { "actor=$actor" }

        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo id
    }

    @Test
    fun `find actors by lastName`() {
        val lastName = "Depp"

        val depp = client.httpGet("/actors?lastName=$lastName")
            .expectBodyList<ActorDTO>()
            .returnResult().responseBody

        log.debug { "actors=$depp" }
        depp.shouldNotBeNull() shouldHaveSize 1
    }

    @Test
    fun `find actors by firstName`() {
        val firstName = "Angelina"

        val angelinas = client.httpGet("/actors?firstName=$firstName")
            .expectBodyList<ActorDTO>()
            .returnResult().responseBody

        log.debug { "actors=$angelinas" }
        angelinas.shouldNotBeNull() shouldHaveSize 2
    }

    @Test
    fun `create new actor`() {
        val actor = newActorDTO()

        val newActor = client
            .httpPost("/actors", actor)
            .expectBody<ActorDTO>()
            .returnResult().responseBody

        log.debug { "newActor=$newActor" }

        newActor.shouldNotBeNull()
        newActor shouldBeEqualTo actor.copy(id = newActor.id)
    }

    @Test
    fun `delete actor`() {
        val actor = newActorDTO()

        val newActor = client
            .httpPost("/actors", actor)
            .expectBody<ActorDTO>()
            .returnResult().responseBody

        log.debug { "newActor=$newActor" }
        newActor.shouldNotBeNull()

        val deletedCount = client
            .httpDelete("/actors/${newActor.id}")
            .expectBody<Int>()
            .returnResult().responseBody

        log.debug { "deletedCount=$deletedCount" }

        deletedCount shouldBeEqualTo 1
    }
}
