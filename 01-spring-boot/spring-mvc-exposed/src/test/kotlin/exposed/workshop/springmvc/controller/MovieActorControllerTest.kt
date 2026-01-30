package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.AbstractSpringMvcTest
import exposed.workshop.springmvc.domain.MovieActorCountDTO
import exposed.workshop.springmvc.domain.MovieWithActorDTO
import exposed.workshop.springmvc.domain.MovieWithProducingActorDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

class MovieActorControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractSpringMvcTest() {

    companion object: KLogging()

    @Test
    fun `get movie with actors`() = runSuspendIO {
        val movieId = 1L

        val movieWithActors = client
            .httpGet("/movie-actors/$movieId")
            .expectStatus().is2xxSuccessful
            .returnResult<MovieWithActorDTO>().responseBody
            .awaitSingle()

        log.debug { "movieWithActors[$movieId]=$movieWithActors" }
        movieWithActors.id shouldBeEqualTo movieId
    }

    @Test
    fun `get movie and actor count - group by movie name`() = runSuspendIO {
        val movieActorCounts = client
            .httpGet("/movie-actors/count")
            .expectStatus().is2xxSuccessful
            .expectBodyList<MovieActorCountDTO>()
            .returnResult().responseBody
            .shouldNotBeNull()

        movieActorCounts.forEach {
            log.debug { "movieActorCount=$it" }
        }
        movieActorCounts.shouldNotBeEmpty()
    }

    @Test
    fun `get movie and acting producer`() = runSuspendIO {
        val movieWithProducers = client
            .httpGet("/movie-actors/acting-producers")
            .expectStatus().is2xxSuccessful
            .expectBodyList<MovieWithProducingActorDTO>()
            .returnResult().responseBody
            .shouldNotBeNull()

        movieWithProducers.forEach {
            log.debug { "movieWithProducer=$it" }
        }
        movieWithProducers.shouldNotBeEmpty()
    }
}
