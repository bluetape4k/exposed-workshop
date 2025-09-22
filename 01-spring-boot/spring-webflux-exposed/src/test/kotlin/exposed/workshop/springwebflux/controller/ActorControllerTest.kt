package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import exposed.workshop.springwebflux.domain.ActorDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

class ActorControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractSpringWebfluxTest() {

    companion object: KLoggingChannel() {
        private fun newActorDTO(): ActorDTO = ActorDTO(
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
            .returnResult<ActorDTO>().responseBody
            .awaitSingle()

        log.debug { "actor=$actor" }

        actor.shouldNotBeNull()
        actor.id shouldBeEqualTo id
    }

    @Test
    fun `find actors by lastName`() = runSuspendIO {
        val lastName = "Depp"

        val depp = client.httpGet("/actors?lastName=$lastName")
            .returnResult<ActorDTO>().responseBody
            .asFlow()
            .toList()

        log.debug { "actors=$depp" }
        depp.shouldNotBeNull() shouldHaveSize 1
    }

    @Test
    fun `find actors by firstName`() = runSuspendIO {
        val firstName = "Angelina"

        val angelinas = client.httpGet("/actors?firstName=$firstName")
            .returnResult<ActorDTO>().responseBody
            .asFlow()
            .toList()

        log.debug { "actors=$angelinas" }
        angelinas.shouldNotBeNull() shouldHaveSize 2
    }

    @Test
    fun `create new actor`() = runSuspendIO {
        val actor = newActorDTO()

        val newActor = client
            .httpPost("/actors", actor)
            .returnResult<ActorDTO>().responseBody
            .awaitSingle()

        log.debug { "newActor=$newActor" }

        newActor.shouldNotBeNull()
        newActor shouldBeEqualTo actor.copy(id = newActor.id)
    }

    @Test
    fun `delete actor`() = runSuspendIO {
        val actor = newActorDTO()

        val newActor = client
            .httpPost("/actors", actor)
            .returnResult<ActorDTO>().responseBody
            .awaitSingle()

        log.debug { "newActor=$newActor" }
        newActor.shouldNotBeNull()

        val deletedCount = client
            .httpDelete("/actors/${newActor.id}")
            .returnResult<Int>().responseBody
            .awaitSingle()

        log.debug { "deletedCount=$deletedCount" }

        deletedCount shouldBeEqualTo 1
    }
}
