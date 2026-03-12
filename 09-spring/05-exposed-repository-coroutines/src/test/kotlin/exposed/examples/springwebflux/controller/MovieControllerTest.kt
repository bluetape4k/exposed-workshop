package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.model.MovieRecord
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpDelete
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

/**
 * Spring WebFlux + Coroutines 환경에서 Movie REST API의 조회, 생성, 삭제 기능을 테스트합니다.
 */
class MovieControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel() {
        private fun newMovieRecord(): MovieRecord = MovieRecord(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    fun `get movie by id`() = runSuspendIO {
        val id = 1L

        val movie = client
            .httpGet("/movies/$id")
            .expectStatus().is2xxSuccessful
            .returnResult<MovieRecord>().responseBody
            .awaitSingle()

        log.debug { "movie=$movie" }

        movie.shouldNotBeNull()
        movie.id shouldBeEqualTo id
    }

    @Test
    fun `search movies by producer name`() = runSuspendIO {
        val producerName = "Johnny"

        val movies = client
            .httpGet("/movies?producerName=$producerName")
            .expectStatus().is2xxSuccessful
            .expectBodyList<MovieRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        movies.size shouldBeEqualTo 2
    }

    @Test
    fun `create new movie`() = runSuspendIO {
        val newMovie = newMovieRecord()

        val saved = client
            .httpPost("/movies", newMovie)
            .expectStatus().is2xxSuccessful
            .returnResult<MovieRecord>().responseBody
            .awaitSingle()

        log.debug { "saved=$saved" }

        saved.shouldNotBeNull() shouldBeEqualTo newMovie.copy(id = saved.id)
    }

    @Test
    fun `delete movie`() = runSuspendIO {
        val newMovie = newMovieRecord()

        val saved = client
            .httpPost("/movies", newMovie)
            .expectStatus().is2xxSuccessful
            .returnResult<MovieRecord>().responseBody
            .awaitSingle()

        val deletedCount = client
            .httpDelete("/movies/${saved.id}")
            .expectStatus().is2xxSuccessful
            .returnResult<Int>().responseBody
            .awaitSingle()

        deletedCount shouldBeEqualTo 1
    }
}
