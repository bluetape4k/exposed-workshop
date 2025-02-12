package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.AbstractSpringMvcTest
import exposed.workshop.springmvc.domain.MovieActorCountDTO
import exposed.workshop.springmvc.domain.MovieWithActorDTO
import exposed.workshop.springmvc.domain.MovieWithProducingActorDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class MovieActorControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractSpringMvcTest() {

    companion object: KLogging()

    @Test
    fun `get movie with actors`() {
        val movieId = 1L

        val movieWithActors = client
            .get()
            .uri("/movie-actors/$movieId")
            .exchange()
            .expectStatus().isOk
            .expectBody<MovieWithActorDTO>()
            .returnResult().responseBody

        log.debug { "movieWithActors[$movieId]=$movieWithActors" }

        movieWithActors.shouldNotBeNull()
        movieWithActors.id shouldBeEqualTo movieId
    }

    @Test
    fun `get movie and actor count - group by movie name`() {
        val movieActorCounts = client
            .get()
            .uri("/movie-actors/count")
            .exchange()
            .expectStatus().isOk
            .expectBody<List<MovieActorCountDTO>>()
            .returnResult().responseBody!!

        movieActorCounts.forEach {
            log.debug { "movieActorCount=$it" }
        }
        movieActorCounts.shouldNotBeEmpty()
    }

    @Test
    fun `get movie and acting producer`() {
        val movieWithProducers = client
            .get()
            .uri("/movie-actors/acting-producers")
            .exchange()
            .expectStatus().isOk
            .expectBody<List<MovieWithProducingActorDTO>>()
            .returnResult().responseBody!!

        movieWithProducers.forEach {
            log.debug { "movieWithProducer=$it" }
        }
        movieWithProducers.shouldNotBeEmpty()
    }
}
