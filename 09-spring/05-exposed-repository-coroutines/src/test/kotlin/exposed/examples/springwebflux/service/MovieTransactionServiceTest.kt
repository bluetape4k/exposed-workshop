package exposed.examples.springwebflux.service

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.dtos.MovieDTO
import exposed.examples.springwebflux.domain.model.toMovieDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.reactor.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional

@Disabled("suspend 함수에 대해 @Transactional 이 적용되지 않습니다. newSuspendedTransaction 을 사용하세요")
class MovieTransactionServiceTest(
    @param:Autowired private val movieService: MovieTransactionalService,
): AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel() {
        private fun newMovieDTO() = MovieDTO(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    @Transactional
    @Commit
    @Test
    fun `reactor 함수에서 @Transactional 적용하면 Transaction이 적용된다`(): Unit = runSuspendIO {
        log.debug { "reactor 함수에서 @Transactional 적용하면 Transaction이 적용된다" }

        val movie = newMovieDTO()
        val movieEntity = movieService.monoSave(movie).awaitSingle()

        val savedMovie = movieEntity.toMovieDTO()
        savedMovie shouldBeEqualTo movie.copy(id = savedMovie.id)
    }
}
