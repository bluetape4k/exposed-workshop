package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.model.MovieActorCountRecord
import exposed.workshop.springwebflux.domain.model.MovieWithActorRecord
import exposed.workshop.springwebflux.domain.model.MovieWithProducingActorRecord
import exposed.workshop.springwebflux.domain.repository.MovieRepository
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("DEPRECATION")
@RestController
@RequestMapping("/movie-actors")
class MovieActorsController(
    private val movieRepository: MovieRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO) {

    companion object: KLoggingChannel()

    @GetMapping("/{movieId}")
    suspend fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorRecord? =
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
            movieRepository.findMoviesWithActingProducers().toFastList()
        }
}
