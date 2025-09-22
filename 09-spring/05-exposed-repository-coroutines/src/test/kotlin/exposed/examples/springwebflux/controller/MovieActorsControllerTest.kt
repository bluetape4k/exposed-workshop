package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.dtos.MovieActorCountDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithActorDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithProducingActorDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

class MovieActorsControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel()

    @Test
    fun `get movie with actors`() = runSuspendIO {
        val movieId = 1L

        val movieWithActors = client
            .httpGet("/movie-actors/$movieId")
            .returnResult<MovieWithActorDTO>().responseBody
            .awaitSingle()

        log.debug { "movieWithActors[$movieId]=$movieWithActors" }

        movieWithActors.shouldNotBeNull()
        movieWithActors.id shouldBeEqualTo movieId
    }

    @Test
    fun `get movie and actor count group by movie name`() = runSuspendIO {
        val movieActorCounts = client
            .httpGet("/movie-actors/count")
            .returnResult<MovieActorCountDTO>().responseBody
            .asFlow()
            .toList()

        movieActorCounts.forEach {
            log.debug { "movieActorCount=$it" }
        }
        movieActorCounts.shouldNotBeNull() shouldHaveSize 4
    }

    @Test
    fun `get movie and acting producer`() = runSuspendIO {
        val movieWithProducers = client
            .httpGet("/movie-actors/acting-producers")
            .returnResult<MovieWithProducingActorDTO>().responseBody
            .asFlow()
            .toList()

        movieWithProducers.forEach {
            log.debug { "movieWithProducer=$it" }
        }
        movieWithProducers shouldHaveSize 1
    }
}
