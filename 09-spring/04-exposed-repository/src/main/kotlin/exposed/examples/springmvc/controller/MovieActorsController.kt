package exposed.examples.springmvc.controller

import exposed.examples.springmvc.domain.model.MovieActorCountRecord
import exposed.examples.springmvc.domain.model.MovieWithActorRecord
import exposed.examples.springmvc.domain.model.MovieWithProducingActorRecord
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
    fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorRecord? {
        return movieRepository.getMovieWithActors(movieId)
    }

    @GetMapping("/count")
    fun getMovieActorsCount(): List<MovieActorCountRecord> {
        return movieRepository.getMovieActorsCount()
    }

    @GetMapping("/acting-producers")
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorRecord> {
        return movieRepository.findMoviesWithActingProducers()
    }
}
