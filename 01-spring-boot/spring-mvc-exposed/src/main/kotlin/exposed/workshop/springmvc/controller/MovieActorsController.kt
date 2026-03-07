package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.domain.model.MovieActorCountRecord
import exposed.workshop.springmvc.domain.model.MovieWithActorRecord
import exposed.workshop.springmvc.domain.model.MovieWithProducingActorRecord
import exposed.workshop.springmvc.domain.repository.MovieRepository
import io.bluetape4k.logging.KLogging
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 영화-배우 관계 조회 REST API 컨트롤러.
 *
 * `/movie-actors` 경로로 영화에 출연한 배우 정보, 출연 수 집계,
 * 프로듀서 겸 배우 목록 등 복합 조회 기능을 제공합니다.
 * 모든 요청은 읽기 전용 트랜잭션으로 처리됩니다.
 *
 * @param movieRepo 영화 데이터 접근 레포지토리
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping("/movie-actors")
class MovieActorsController(private val movieRepo: MovieRepository) {

    companion object: KLogging()

    /**
     * 특정 영화와 출연 배우 목록을 함께 조회합니다.
     *
     * HTTP GET `/movie-actors/{movieId}`
     *
     * @param movieId 조회할 영화의 고유 식별자
     * @return 영화 정보와 배우 목록이 담긴 [MovieWithActorRecord], 존재하지 않으면 null
     */
    @GetMapping("/{movieId}")
    fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorRecord? {
        return movieRepo.getMovieWithActors(movieId)
    }

    /**
     * 영화별 출연 배우 수를 집계하여 반환합니다.
     *
     * HTTP GET `/movie-actors/count`
     *
     * @return 각 영화의 배우 출연 수 정보가 담긴 [MovieActorCountRecord] 목록
     */
    @GetMapping("/count")
    fun getMovieActorsCount(): List<MovieActorCountRecord> {
        return movieRepo.getMovieActorsCount()
    }

    /**
     * 프로듀서가 직접 배우로도 출연한 영화 목록을 반환합니다.
     *
     * HTTP GET `/movie-actors/acting-producers`
     *
     * @return 프로듀서 겸 배우 정보가 담긴 [MovieWithProducingActorRecord] 목록
     */
    @GetMapping("/acting-producers")
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorRecord> {
        return movieRepo.findMoviesWithActingProducers()
    }
}
