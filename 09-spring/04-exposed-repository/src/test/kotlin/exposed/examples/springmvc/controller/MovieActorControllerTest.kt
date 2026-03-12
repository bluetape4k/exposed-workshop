package exposed.examples.springmvc.controller

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.model.MovieActorCountRecord
import exposed.examples.springmvc.domain.model.MovieWithActorRecord
import exposed.examples.springmvc.domain.model.MovieWithProducingActorRecord
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

/**
 * Spring MVC 환경에서 영화-배우 관계 REST API(조회, 집계, 제작-배우 조회)를 테스트합니다.
 */
class MovieActorControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractExposedRepositoryTest() {

    companion object: KLogging()

    @Test
    fun `get movie with actors`() = runSuspendIO {
        val movieId = 1L

        val movieWithActors = client
            .httpGet("/movie-actors/$movieId")
            .expectStatus().is2xxSuccessful
            .returnResult<MovieWithActorRecord>().responseBody
            .awaitSingle()

        log.debug { "movieWithActors[$movieId]=$movieWithActors" }

        movieWithActors.shouldNotBeNull()
        movieWithActors.id shouldBeEqualTo movieId
    }

    @Test
    fun `get movie and actor count - group by movie name`() = runSuspendIO {
        val movieActorCounts = client
            .httpGet("/movie-actors/count")
            .expectStatus().is2xxSuccessful
            .expectBodyList<MovieActorCountRecord>()
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
            .expectBodyList<MovieWithProducingActorRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        movieWithProducers.forEach {
            log.debug { "movieWithProducer=$it" }
        }
        movieWithProducers.shouldNotBeEmpty()
    }
}
