package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.model.MovieRecord
import exposed.workshop.springwebflux.domain.model.toMovieRecord
import exposed.workshop.springwebflux.domain.repository.MovieRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("DEPRECATION")
@RestController
@RequestMapping("/movies")
class MovieController(
    private val movieRepository: MovieRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO) {

    companion object: KLoggingChannel()

    @GetMapping("/{id}")
    suspend fun getMovieById(@PathVariable("id") movieId: Long): MovieRecord? =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.findById(movieId)?.toMovieRecord()
        }

    @GetMapping
    suspend fun searchMovies(request: ServerHttpRequest): List<MovieRecord> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()
        return when {
            params.isEmpty() -> newSuspendedTransaction(readOnly = true) {
                movieRepository.findAll().map { it.toMovieRecord() }
            }
            else -> newSuspendedTransaction(readOnly = true) {
                movieRepository.searchMovie(params).map { it.toMovieRecord() }
            }
        }
    }

    @PostMapping
    suspend fun createMovie(@RequestBody movie: MovieRecord): MovieRecord =
        newSuspendedTransaction {
            movieRepository.create(movie).toMovieRecord()
        }

    @DeleteMapping("/{id}")
    suspend fun deleteMovie(@PathVariable("id") movieId: Long): Int =
        newSuspendedTransaction {
            movieRepository.deleteById(movieId)
        }
}
