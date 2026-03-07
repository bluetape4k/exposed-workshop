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
 * `/movies` 경로로 영화 정보의 조회, 생성, 삭제 기능을 제공합니다.
 * 기본적으로 읽기 전용 트랜잭션으로 동작하며, 쓰기 작업은 별도로 트랜잭션을 지정합니다.
 *
 * @param movieRepository 영화 데이터 접근 레포지토리
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping("/movies")
class MovieController(private val movieRepository: MovieRepository) {

    companion object: KLogging()

    /**
     * ID로 영화를 조회합니다.
     *
     * HTTP GET `/movies/{id}`
     *
     * @param movieId 조회할 영화의 고유 식별자
     * @return 해당 ID의 [MovieRecord], 존재하지 않으면 null
     */
    @GetMapping("/{id}")
    fun getMovieById(@PathVariable("id") movieId: Long): MovieRecord? {
        return movieRepository.findById(movieId)
    }

    /**
     * 쿼리 파라미터를 기반으로 영화를 검색합니다.
     *
     * HTTP GET `/movies?name=...&producerName=...`
     *
     * 지원 필터: `id`, `name`, `producerName`, `releaseDate`
     *
     * @param request HTTP 서블릿 요청 (쿼리 파라미터 포함)
     * @return 조건에 맞는 [MovieRecord] 목록
     */
    @GetMapping
    fun searchMovies(request: HttpServletRequest): List<MovieRecord> {
        val params = request.parameterMap.map { it.key to it.value.firstOrNull() }.toMap()
        log.debug { "Search Movies... params=$params" }

        return movieRepository.searchMovies(params)
    }

    /**
     * 새로운 영화를 생성합니다.
     *
     * HTTP POST `/movies`
     *
     * @param movieRecord 생성할 영화 정보가 담긴 [MovieRecord] (id는 무시됨)
     * @return 생성된 영화의 [MovieRecord] (서버에서 할당된 id 포함), 실패 시 null
     */
    @Transactional
    @PostMapping
    fun createMovie(@RequestBody movieRecord: MovieRecord): MovieRecord? {
        return movieRepository.create(movieRecord)
    }

    /**
     * ID로 영화를 삭제합니다.
     *
     * HTTP DELETE `/movies/{id}`
     *
     * @param movieId 삭제할 영화의 고유 식별자
     * @return 삭제된 행 수 (성공 시 1, 존재하지 않으면 0)
     */
    @Transactional
    @DeleteMapping("/{id}")
    fun deleteMovieById(@PathVariable("id") movieId: Long): Int {
        return movieRepository.deleteById(movieId)
    }
}
