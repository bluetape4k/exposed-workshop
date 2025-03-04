package exposed.examples.springmvc.controller

import exposed.examples.springmvc.domain.dtos.MovieDTO
import exposed.examples.springmvc.domain.dtos.toMovieDTO
import exposed.examples.springmvc.domain.repository.MovieExposedRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import jakarta.servlet.http.HttpServletRequest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Transactional(readOnly = true)
@RequestMapping("/movies")
class MovieController(private val movieRepository: MovieExposedRepository) {

    companion object: KLogging()

    @GetMapping("/{id}")
    fun getMovieById(@PathVariable("id") movieId: Long): MovieDTO? {
        return movieRepository.findByIdOrNull(movieId)?.toMovieDTO()
    }

    @GetMapping
    fun searchMovies(request: HttpServletRequest): List<MovieDTO> {
        val params = request.parameterMap.map { it.key to it.value.firstOrNull() }.toMap()
        log.debug { "Search Movies... params=$params" }

        return movieRepository.searchMovies(params).map { it.toMovieDTO() }
    }

    @Transactional
    @PostMapping
    fun createMovie(@RequestBody movieDTO: MovieDTO): MovieDTO {
        return movieRepository.create(movieDTO).toMovieDTO()
    }

    @Transactional
    @DeleteMapping("/{id}")
    fun deleteMovieById(@PathVariable("id") movieId: Long): Int {
        return movieRepository.deleteById(movieId)
    }
}
