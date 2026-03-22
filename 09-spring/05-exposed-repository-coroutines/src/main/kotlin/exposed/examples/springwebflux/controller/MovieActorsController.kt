package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.domain.model.MovieActorCountRecord
import exposed.examples.springwebflux.domain.model.MovieWithActorRecord
import exposed.examples.springwebflux.domain.model.MovieWithProducingActorRecord
import exposed.examples.springwebflux.domain.repository.MovieExposedRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("DEPRECATION")
@RestController
@RequestMapping("/movie-actors")
class MovieActorsController(
    private val movieRepository: MovieExposedRepository,
) {
    companion object : KLoggingChannel()

    @GetMapping("/{movieId}")
    suspend fun getMovieWithActors(
        @PathVariable movieId: Long,
    ): MovieWithActorRecord? =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.getMovieWithActors(movieId)
        }

    @GetMapping("/count")
    suspend fun getMovieActorsCount(): List<MovieActorCountRecord> =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.getMovieActorsCount()
        }

    @GetMapping("/acting-producers")
    suspend fun findMoviesWithActingProducers(): List<MovieWithProducingActorRecord> =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.findMoviesWithActingProducers()
        }
}
