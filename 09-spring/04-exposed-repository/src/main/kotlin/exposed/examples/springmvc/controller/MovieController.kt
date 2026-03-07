package exposed.examples.springmvc.controller

import exposed.examples.springmvc.domain.model.MovieRecord
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

/**
 * 영화(Movie) 정보를 관리하는 REST 컨트롤러.
 *
 * 영화 조회, 검색, 생성, 삭제 기능을 HTTP 엔드포인트로 제공합니다.
 * 기본적으로 읽기 전용 트랜잭션을 사용하며, 쓰기 작업은 별도의 트랜잭션을 적용합니다.
 *
 * @property movieRepository 영화 데이터 접근을 위한 Exposed 리포지토리
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping("/movies")
class MovieController(private val movieRepository: MovieExposedRepository) {

    companion object: KLogging()

    /**
     * ID로 영화를 조회합니다.
     *
     * @param movieId 조회할 영화의 ID
     * @return 조회된 영화 레코드, 존재하지 않으면 null
     */
    @GetMapping("/{id}")
    fun getMovieById(@PathVariable("id") movieId: Long): MovieRecord? {
        return movieRepository.findByIdOrNull(movieId)
    }

    /**
     * 쿼리 파라미터를 기반으로 영화를 검색합니다.
     *
     * @param request 검색 조건이 담긴 HTTP 요청
     * @return 검색 조건에 맞는 영화 레코드 목록
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
     * @param movieRecord 생성할 영화 정보
     * @return 생성된 영화 레코드 (ID 포함)
     */
    @Transactional
    @PostMapping
    fun createMovie(@RequestBody movieRecord: MovieRecord): MovieRecord {
        return movieRepository.create(movieRecord)
    }

    /**
     * ID로 영화를 삭제합니다.
     *
     * @param movieId 삭제할 영화의 ID
     * @return 삭제된 레코드 수
     */
    @Transactional
    @DeleteMapping("/{id}")
    fun deleteMovieById(@PathVariable("id") movieId: Long): Int {
        return movieRepository.deleteById(movieId)
    }
}
