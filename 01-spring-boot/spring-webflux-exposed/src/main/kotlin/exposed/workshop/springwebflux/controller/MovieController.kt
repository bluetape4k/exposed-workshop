package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.MovieDTO
import exposed.workshop.springwebflux.domain.repository.MovieRepository
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
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
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLogging()

    @GetMapping("/{id}")
    suspend fun getMovieById(@PathVariable("id") movieId: Long): MovieDTO? {
        return movieRepository.findById(movieId)
    }

    @GetMapping
    suspend fun searchMovies(request: ServerHttpRequest): List<MovieDTO> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()
        return when {
            params.isEmpty() -> emptyList()
            else -> movieRepository.searchMovie(params).toList()
        }
    }

    @PostMapping
    suspend fun createMovie(@RequestBody movie: MovieDTO): MovieDTO {
        return movieRepository.create(movie)
    }

    @DeleteMapping("/{id}")
    suspend fun deleteMovie(@PathVariable("id") movieId: Long): Int {
        return movieRepository.deleteById(movieId)
    }

}
