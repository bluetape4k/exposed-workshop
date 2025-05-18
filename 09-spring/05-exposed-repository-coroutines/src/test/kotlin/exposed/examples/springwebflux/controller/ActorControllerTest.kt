package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.dtos.ActorDTO
import exposed.examples.springwebflux.domain.repository.ActorRepositoryTest.Companion.newActorDTO
import io.bluetape4k.logging.coroutines.KLoggingChannel
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
): AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel()

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
