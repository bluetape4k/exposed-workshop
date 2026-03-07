package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.domain.model.MovieRecord
import exposed.examples.springwebflux.domain.repository.MovieExposedRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 영화(Movie) 정보를 관리하는 코루틴 기반 REST 컨트롤러.
 *
 * Spring WebFlux 환경에서 코루틴 suspend 함수를 사용하여 비동기적으로
 * 영화 조회, 검색, 생성, 삭제 기능을 HTTP 엔드포인트로 제공합니다.
 * [newSuspendedTransaction]을 통해 Exposed 트랜잭션을 코루틴과 통합합니다.
 *
 * @property movieRepository 영화 데이터 접근을 위한 Exposed 리포지토리
 */
@Suppress("DEPRECATION")
@RestController
@RequestMapping("/movies")
class MovieController(
    private val movieRepository: MovieExposedRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel()

    /**
     * ID로 영화를 조회합니다.
     *
     * @param movieId 조회할 영화의 ID
     * @return 조회된 영화 레코드, 존재하지 않으면 null
     */
    @GetMapping("/{id}")
    suspend fun getMovieById(@PathVariable("id") movieId: Long): MovieRecord? =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.findByIdOrNull(movieId)
        }

    /**
     * 쿼리 파라미터를 기반으로 영화를 검색합니다.
     *
     * @param request 검색 조건이 담긴 서버 HTTP 요청
     * @return 검색 조건에 맞는 영화 레코드 목록, 파라미터가 없으면 빈 목록
     */
    @GetMapping
    suspend fun searchMovies(request: ServerHttpRequest): List<MovieRecord> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()
        return when {
            params.isEmpty() -> emptyList()
            else -> newSuspendedTransaction(readOnly = true) {
                movieRepository.searchMovie(params)
            }
        }
    }

    /**
     * 새로운 영화를 생성합니다.
     *
     * @param movie 생성할 영화 정보
     * @return 생성된 영화 레코드 (ID 포함)
     */
    @PostMapping
    suspend fun createMovie(@RequestBody movie: MovieRecord): MovieRecord =
        newSuspendedTransaction {
            movieRepository.create(movie)
        }

    /**
     * ID로 영화를 삭제합니다.
     *
     * @param movieId 삭제할 영화의 ID
     * @return 삭제된 레코드 수
     */
    @DeleteMapping("/{id}")
    suspend fun deleteMovie(@PathVariable("id") movieId: Long): Int =
        newSuspendedTransaction {
            movieRepository.deleteById(movieId)
        }
}
