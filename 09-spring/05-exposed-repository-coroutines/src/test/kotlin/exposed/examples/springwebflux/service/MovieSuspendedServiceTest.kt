package exposed.examples.springwebflux.service

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.dtos.MovieDTO
import exposed.examples.springwebflux.domain.model.toMovieDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MovieSuspendedServiceTest(
    @Autowired private val movieService: MovieTransactionalService,
): AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel() {
        private fun newMovieDTO() = MovieDTO(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Test
    fun `newSuspendedTransaction 함수 안에서 사용하면 Transaction이 적용된다`(): Unit = runSuspendIO {
        log.debug { "reactor 함수에서 @Transactional 적용하면 Transaction이 적용된다" }

        newSuspendedTransaction {
            val movie = newMovieDTO()
            val movieEntity = movieService.suspendedSave(movie)

            val savedMovie = movieEntity.toMovieDTO()
            savedMovie shouldBeEqualTo movie.copy(id = savedMovie.id)
        }
    }
}
