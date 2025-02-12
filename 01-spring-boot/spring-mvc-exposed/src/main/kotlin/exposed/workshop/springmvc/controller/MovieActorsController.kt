package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.domain.MovieActorCountDTO
import exposed.workshop.springmvc.domain.MovieWithActorDTO
import exposed.workshop.springmvc.domain.MovieWithProducingActorDTO
import exposed.workshop.springmvc.domain.repository.MovieRepository
import io.bluetape4k.logging.KLogging
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Transactional(readOnly = true)
@RequestMapping("/movie-actors")
class MovieActorsController(private val movieRepo: MovieRepository) {

    companion object: KLogging()

    @GetMapping("/{movieId}")
    fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorDTO? {
        return movieRepo.getMovieWithActors(movieId)
    }

    @GetMapping("/count")
    fun getMovieActorsCount(): List<MovieActorCountDTO> {
        return movieRepo.getMovieActorsCount()
    }

    @GetMapping("/acting-producers")
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorDTO> {
        return movieRepo.findMoviesWithActingProducers()
    }
}
