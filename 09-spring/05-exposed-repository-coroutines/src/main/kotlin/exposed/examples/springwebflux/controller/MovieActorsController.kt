package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.domain.dtos.MovieActorCountDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithActorDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithProducingActorDTO
import exposed.examples.springwebflux.domain.repository.MovieExposedRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/movie-actors")
class MovieActorsController(
    private val movieRepository: MovieExposedRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel()

    @GetMapping("/{movieId}")
    suspend fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorDTO? =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.getMovieWithActors(movieId)
        }

    @GetMapping("/count")
    suspend fun getMovieActorsCount(): List<MovieActorCountDTO> =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.getMovieActorsCount()
        }

    @GetMapping("/acting-producers")
    suspend fun findMoviesWithActingProducers(): List<MovieWithProducingActorDTO> =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.findMoviesWithActingProducers()
        }
}
