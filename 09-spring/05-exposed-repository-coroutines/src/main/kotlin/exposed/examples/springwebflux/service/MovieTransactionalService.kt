package exposed.examples.springwebflux.service

import exposed.examples.springwebflux.domain.dtos.MovieDTO
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
    fun monoSave(movieDto: MovieDTO): Mono<MovieEntity> {
        log.debug { "save movieDto: $movieDto" }
        // movieRepository.create(movieDto)

        return Mono.fromCallable {
            MovieEntity.new {
                name = movieDto.name
                producerName = movieDto.producerName
                if (movieDto.releaseDate.isNotBlank()) {
                    releaseDate = LocalDate.parse(movieDto.releaseDate)
                }
            }
        }
    }

    suspend fun suspendedSave(movieDto: MovieDTO): MovieEntity {
        log.debug { "suspendedSave movieDto: $movieDto" }
        return movieRepository.create(movieDto)
    }
}
