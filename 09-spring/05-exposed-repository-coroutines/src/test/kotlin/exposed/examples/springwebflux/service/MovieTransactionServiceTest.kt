package exposed.examples.springwebflux.service

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.model.MovieRecord
import exposed.examples.springwebflux.domain.model.toMovieRecord
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

/**
 * suspend 함수에 `@Transactional`이 적용되지 않음을 확인하는 비활성화 테스트입니다.
 * 코루틴에서는 `newSuspendedTransaction`을 사용해야 합니다.
 */
@Disabled("suspend 함수에 대해 @Transactional 이 적용되지 않습니다. newSuspendedTransaction 을 사용하세요")
class MovieTransactionServiceTest(
    @param:Autowired private val movieService: MovieTransactionalService,
): AbstractCoroutineExposedRepositoryTest() {

    companion object: KLoggingChannel() {
        private fun newMovieRecord() = MovieRecord(
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

        val movie = newMovieRecord()
        val movieEntity = movieService.monoSave(movie).awaitSingle()

        val savedMovie = movieEntity.toMovieRecord()
        savedMovie shouldBeEqualTo movie.copy(id = savedMovie.id)
    }
}
