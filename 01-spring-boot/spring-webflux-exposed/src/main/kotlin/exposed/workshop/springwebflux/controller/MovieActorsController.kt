package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.MovieActorCountDTO
import exposed.workshop.springwebflux.domain.MovieWithActorDTO
import exposed.workshop.springwebflux.domain.MovieWithProducingActorDTO
import exposed.workshop.springwebflux.domain.repository.MovieRepository
import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/movie-actors")
class MovieActorsController(
    private val movieRepository: MovieRepository,
): CoroutineScope by CoroutineScope(Dispatchers.VT) {

    companion object: KLogging()

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
        }.toList()
}
