package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.domain.dtos.MovieActorCountDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithActorDTO
import exposed.examples.springwebflux.domain.dtos.MovieWithProducingActorDTO
import exposed.examples.springwebflux.domain.repository.MovieRepository
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/movie-actors")
@Transactional(readOnly = true)
class MovieActorsController(
    private val movieRepository: MovieRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO) {

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
        }
}
