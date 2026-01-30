package exposed.examples.springmvc.controller

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.dtos.MovieDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

class MovieControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractExposedRepositoryTest() {

    companion object: KLogging()

    @Test
    fun `get movie by id`() = runSuspendIO {
        val id = 1L

        val movie = client
            .httpGet("/movies/$id")
            .expectStatus().is2xxSuccessful
            .returnResult<MovieDTO>().responseBody
            .awaitSingle()

        log.debug { "movie[$id]=$movie" }

        movie.shouldNotBeNull()
        movie.id shouldBeEqualTo id
    }

    @Test
    fun `search movies by producer name`() = runSuspendIO {
        val producerName = "Johnny"

        val movies = client
            .httpGet("/movies?producerName=$producerName")
            .expectStatus().is2xxSuccessful
            .expectBodyList<MovieDTO>()
            .returnResult().responseBody
            .shouldNotBeNull()

        movies.size shouldBeEqualTo 2
    }
}
