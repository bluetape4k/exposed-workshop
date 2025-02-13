package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import exposed.workshop.springwebflux.domain.MovieActorCountDTO
import exposed.workshop.springwebflux.domain.MovieWithActorDTO
import exposed.workshop.springwebflux.domain.MovieWithProducingActorDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class MovieActorsControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractSpringWebfluxTest() {

    companion object: KLogging()

    @Test
    fun `get movie with actors`() {
        val movieId = 1L

        val movieWithActors = client
            .httpGet("/movie-actors/$movieId")
            .expectBody<MovieWithActorDTO>()
            .returnResult().responseBody

        log.debug { "movieWithActors[$movieId]=$movieWithActors" }

        movieWithActors.shouldNotBeNull()
        movieWithActors.id shouldBeEqualTo movieId
    }

    @Test
    fun `get movie and actor count group by movie name`() {
        val movieActorCounts = client
            .httpGet("/movie-actors/count")
            .expectBody<List<MovieActorCountDTO>>()
            .returnResult().responseBody!!

        movieActorCounts.forEach {
            log.debug { "movieActorCount=$it" }
        }
        movieActorCounts.shouldNotBeNull() shouldHaveSize 4
    }

    @Test
    fun `get movie and acting producer`() {
        val movieWithProducers = client
            .httpGet("/movie-actors/acting-producers")
            .expectBody<List<MovieWithProducingActorDTO>>()
            .returnResult().responseBody!!

        movieWithProducers.forEach {
            log.debug { "movieWithProducer=$it" }
        }
        movieWithProducers shouldHaveSize 1
    }
}
