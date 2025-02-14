package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.MovieActorCountDTO
import exposed.workshop.springwebflux.domain.MovieWithActorDTO
import exposed.workshop.springwebflux.domain.MovieWithProducingActorDTO
import exposed.workshop.springwebflux.domain.repository.MovieRepository
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/movie-actors")
class MovieActorsController(
    private val movieRepository: MovieRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLogging()

    @GetMapping("/{movieId}")
    suspend fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorDTO? {
        return movieRepository.getMovieWithActors(movieId)
    }

    @GetMapping("/count")
    suspend fun getMovieActorsCount(): List<MovieActorCountDTO> {
        return movieRepository.getMovieActorsCount()
    }

    @GetMapping("/acting-producers")
    suspend fun findMoviesWithActingProducers(): List<MovieWithProducingActorDTO> {
        return movieRepository.findMoviesWithActingProducers().toList()
    }
}
