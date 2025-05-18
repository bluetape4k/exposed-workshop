package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.dtos.MovieDTO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

class MovieControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel() {
        private fun newMovieDTO(): MovieDTO = MovieDTO(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    fun `get movie by id`() {
        val id = 1L

        val movie = client
            .httpGet("/movies/$id")
            .expectBody<MovieDTO>()
            .returnResult().responseBody

        log.debug { "movie=$movie" }

        movie.shouldNotBeNull()
        movie.id shouldBeEqualTo id
    }

    @Test
    fun `search movies by producer name`() {
        val producerName = "Johnny"

        val movies = client.httpGet("/movies?producerName=$producerName")
            .expectBody<List<MovieDTO>>()
            .returnResult().responseBody!!

        movies.size shouldBeEqualTo 2
    }

    @Test
    fun `create new movie`() {
        val newMovie = newMovieDTO()

        val saved = client
            .httpPost("/movies", newMovie)
            .expectBody<MovieDTO>()
            .returnResult().responseBody

        log.debug { "saved=$saved" }

        saved.shouldNotBeNull() shouldBeEqualTo newMovie.copy(id = saved.id)
    }

    @Test
    fun `delete movie`() {
        val newMovie = newMovieDTO()

        val saved = client
            .httpPost("/movies", newMovie)
            .expectBody<MovieDTO>()
            .returnResult().responseBody!!

        val deletedCount = client.httpDelete("/movies/${saved.id}")
            .expectBody<Int>()
            .returnResult().responseBody

        deletedCount shouldBeEqualTo 1
    }
}
