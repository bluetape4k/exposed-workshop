package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import exposed.workshop.springwebflux.domain.MovieDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult


class MovieControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractSpringWebfluxTest() {

    companion object: KLogging() {
        private fun newMovieDTO(): MovieDTO = MovieDTO(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).atTime(0, 0).toString()
        )
    }

    @Test
    fun `get movie by id`() = runSuspendIO {
        val id = 1L

        val movie = client
            .httpGet("/movies/$id")
            .returnResult<MovieDTO>().responseBody
            .awaitFirstOrNull()

        log.debug { "movie=$movie" }

        movie.shouldNotBeNull()
        movie.id shouldBeEqualTo id
    }

    @Test
    fun `search movies by producer name`() = runSuspendIO {
        val producerName = "Johnny"

        val movies = client.httpGet("/movies?producerName=$producerName")
            .returnResult<MovieDTO>().responseBody
            .asFlow()
            .toList()

        movies.size shouldBeEqualTo 2
    }

    @Test
    fun `create new movie`() = runSuspendIO {
        val newMovie = newMovieDTO()

        val saved = client
            .httpPost("/movies", newMovie)
            .returnResult<MovieDTO>().responseBody
            .awaitFirstOrNull()

        log.debug { "saved=$saved" }
        saved.shouldNotBeNull() shouldBeEqualTo newMovie.copy(id = saved.id)
    }

    @Test
    fun `delete movie`() = runSuspendIO {
        val newMovie = newMovieDTO()

        val saved = client
            .httpPost("/movies", newMovie)
            .returnResult<MovieDTO>().responseBody
            .awaitFirst()

        val deletedCount = client
            .httpDelete("/movies/${saved.id}")
            .returnResult<Int>().responseBody
            .awaitFirst()

        deletedCount shouldBeEqualTo 1
    }
}
