package exposed.examples.springmvc.controller

import exposed.examples.springmvc.domain.dtos.MovieActorCountDTO
import exposed.examples.springmvc.domain.dtos.MovieWithActorDTO
import exposed.examples.springmvc.domain.dtos.MovieWithProducingActorDTO
import exposed.examples.springmvc.domain.repository.MovieExposedRepository
import io.bluetape4k.logging.KLogging
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Transactional(readOnly = true)
@RequestMapping("/movie-actors")
class MovieActorsController(private val movieRepository: MovieExposedRepository) {

    companion object: KLogging()

    @GetMapping("/{movieId}")
    fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorDTO? {
        return movieRepository.getMovieWithActors(movieId)
    }

    @GetMapping("/count")
    fun getMovieActorsCount(): List<MovieActorCountDTO> {
        return movieRepository.getMovieActorsCount()
    }

    @GetMapping("/acting-producers")
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorDTO> {
        return movieRepository.findMoviesWithActingProducers()
    }
}
