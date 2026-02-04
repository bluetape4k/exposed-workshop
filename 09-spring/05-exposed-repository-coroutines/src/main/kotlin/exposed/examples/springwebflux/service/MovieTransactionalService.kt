package exposed.examples.springwebflux.service

import exposed.examples.springwebflux.domain.model.MovieRecord
import exposed.examples.springwebflux.domain.model.MovieSchema.MovieEntity
import exposed.examples.springwebflux.domain.repository.MovieExposedRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.LocalDate

/**
 * `@Transactional` 은 일반적으로 Reactive 함수에 적용됩니다만, Exposed 의 suspended 함수에 대해서는 적용되지 않는다.
 */
@Component
class MovieTransactionalService(
    private val movieRepository: MovieExposedRepository,
) {

    companion object: KLoggingChannel()

    @Transactional
    fun monoSave(movieRecord: MovieRecord): Mono<MovieEntity> {
        log.debug { "save movieDto: $movieRecord" }
        // movieRepository.create(movieDto)

        return Mono.fromCallable {
            MovieEntity.new {
                name = movieRecord.name
                producerName = movieRecord.producerName
                if (movieRecord.releaseDate.isNotBlank()) {
                    releaseDate = LocalDate.parse(movieRecord.releaseDate)
                }
            }
        }
    }

    suspend fun suspendedSave(movieRecord: MovieRecord): MovieRecord {
        log.debug { "suspendedSave movieDto: $movieRecord" }
        return movieRepository.create(movieRecord)
    }
}
