package exposed.examples.springmvc.controller

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.dtos.MovieDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class MovieControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractExposedRepositoryTest() {

    companion object: KLogging()

    @Test
    fun `get movie by id`() {
        val id = 1L

        val movie = client
            .get()
            .uri("/movies/$id")
            .exchange()
            .expectStatus().isOk
            .expectBody<MovieDTO>()
            .returnResult().responseBody

        log.debug { "movie[$id]=$movie" }

        movie.shouldNotBeNull()
        movie.id shouldBeEqualTo id
    }

    @Test
    fun `search movies by producer name`() {
        val producerName = "Johnny"

        val movies = client
            .get()
            .uri("/movies?producerName=$producerName")
            .exchange()
            .expectStatus().isOk
            .expectBody<List<MovieDTO>>()
            .returnResult().responseBody!!

        movies.size shouldBeEqualTo 2
    }

}
