package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.domain.dtos.MovieDTO
import exposed.examples.springwebflux.domain.model.toMovieDTO
import exposed.examples.springwebflux.domain.repository.MovieExposedRepository
import io.bluetape4k.logging.KLogging
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
    private val movieRepository: MovieExposedRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO) {

    companion object: KLogging()

    @GetMapping("/{id}")
    suspend fun getMovieById(@PathVariable("id") movieId: Long): MovieDTO? =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.findByIdOrNull(movieId)?.toMovieDTO()
        }

    @GetMapping
    suspend fun searchMovies(request: ServerHttpRequest): List<MovieDTO> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()
        return when {
            params.isEmpty() -> emptyList()
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
