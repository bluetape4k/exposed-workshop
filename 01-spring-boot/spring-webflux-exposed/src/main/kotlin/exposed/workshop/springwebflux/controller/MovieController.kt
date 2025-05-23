package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.MovieDTO
import exposed.workshop.springwebflux.domain.repository.MovieRepository
import exposed.workshop.springwebflux.domain.toMovieDTO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/movies")
class MovieController(
    private val movieRepository: MovieRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO) {

    companion object: KLoggingChannel()

    @GetMapping("/{id}")
    suspend fun getMovieById(@PathVariable("id") movieId: Long): MovieDTO? =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.findById(movieId)?.toMovieDTO()
        }

    @GetMapping
    suspend fun searchMovies(request: ServerHttpRequest): List<MovieDTO> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()
        return when {
            params.isEmpty() -> newSuspendedTransaction(readOnly = true) {
                movieRepository.findAll().map { it.toMovieDTO() }
            }
            else -> newSuspendedTransaction(readOnly = true) {
                movieRepository.searchMovie(params).map { it.toMovieDTO() }
            }
        }
    }

    @PostMapping
    suspend fun createMovie(@RequestBody movie: MovieDTO): MovieDTO =
        newSuspendedTransaction {
            movieRepository.create(movie).toMovieDTO()
        }

    @DeleteMapping("/{id}")
    suspend fun deleteMovie(@PathVariable("id") movieId: Long): Int =
        newSuspendedTransaction {
            movieRepository.deleteById(movieId)
        }
}
