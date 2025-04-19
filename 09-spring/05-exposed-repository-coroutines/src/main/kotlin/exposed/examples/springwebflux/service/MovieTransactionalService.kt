package exposed.examples.springwebflux.service

import exposed.examples.springwebflux.domain.dtos.MovieDTO
import exposed.examples.springwebflux.domain.model.MovieSchema.MovieEntity
import exposed.examples.springwebflux.domain.repository.MovieRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

/**
 * `@Transactional` 은 일반적으로 Reactive 함수에 적용됩니다만, Exposed 의 suspended 함수에 대해서는 적용되지 않는다.
 */
@Component
class MovieTransactionalService(
    private val movieRepository: MovieRepository,
) {

    companion object: KLogging()

    @Transactional
    fun save(movieDto: MovieDTO): Mono<MovieEntity> = mono {
        log.debug { "save movieDto: $movieDto" }
        movieRepository.create(movieDto)
    }
}
