package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.AbstractSpringMvcTest
import exposed.workshop.springmvc.domain.model.MovieRecord
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

class MovieControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractSpringMvcTest() {

    companion object: KLogging()

    @Test
    fun `get movie by id`() = runSuspendIO {
        val id = 1L

        val movie = client
            .httpGet("/movies/$id")
            .expectStatus().is2xxSuccessful
            .returnResult<MovieRecord>().responseBody
            .awaitSingle()

        log.debug { "movie[$id]=$movie" }
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

        movies shouldHaveSize 2
    }

    @Test
    fun `search movies ignores invalid release date parameter`() = runSuspendIO {
        val movies = client
            .httpGet("/movies?releaseDate=invalid-timestamp")
            .expectStatus().is2xxSuccessful
            .expectBodyList<MovieRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        movies.shouldNotBeEmpty()
    }
}
