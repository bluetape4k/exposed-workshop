package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.domain.model.MovieRecord
import exposed.workshop.springmvc.domain.repository.MovieRepository
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

/**
 * 영화(Movie) 관련 REST API 컨트롤러.
 *
 * `/movies` 경로로 영화 조회, 검색, 생성, 삭제 기능을 제공합니다.
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping("/movies")
class MovieController(private val movieRepository: MovieRepository) {

    companion object: KLogging()

    /**
     * ID로 영화를 조회합니다.
     *
     * @param movieId 조회할 영화 ID
     * @return 영화 정보, 존재하지 않으면 null
     */
    @GetMapping("/{id}")
    fun getMovieById(@PathVariable("id") movieId: Long): MovieRecord? {
        return movieRepository.findById(movieId)
    }

    /**
     * 요청 파라미터를 기반으로 영화를 검색합니다.
     *
     * @param request 검색 조건이 포함된 HTTP 요청
     * @return 조건에 맞는 영화 목록
     */
    @GetMapping
    fun searchMovies(request: HttpServletRequest): List<MovieRecord> {
        val params = request.parameterMap.map { it.key to it.value.firstOrNull() }.toMap()
        log.debug { "Search Movies... params=$params" }

        return movieRepository.searchMovies(params)
    }

    /**
     * 새 영화를 생성합니다.
     *
     * @param movieRecord 생성할 영화 정보
     * @return 생성된 영화 정보 (ID 포함)
     */
    @Transactional
    @PostMapping
    fun createMovie(@RequestBody movieRecord: MovieRecord): MovieRecord? {
        return movieRepository.create(movieRecord)
    }

    /**
     * ID로 영화를 삭제합니다.
     *
     * @param movieId 삭제할 영화 ID
     * @return 삭제된 행 수
     */
    @Transactional
    @DeleteMapping("/{id}")
    fun deleteMovieById(@PathVariable("id") movieId: Long): Int {
        return movieRepository.deleteById(movieId)
    }
}
