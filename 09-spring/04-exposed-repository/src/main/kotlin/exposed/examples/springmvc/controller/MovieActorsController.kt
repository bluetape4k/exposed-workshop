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

/**
 * 영화와 배우 연관 관계를 조회하는 REST 컨트롤러.
 *
 * 영화에 출연한 배우 목록, 영화별 배우 수, 제작자 겸 배우 정보 등을 제공합니다.
 *
 * @property movieRepository 영화 데이터 접근을 위한 Exposed 리포지토리
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping("/movie-actors")
class MovieActorsController(private val movieRepository: MovieExposedRepository) {

    companion object: KLogging()

    /**
     * 특정 영화에 출연한 배우 목록을 포함한 영화 정보를 반환합니다.
     *
     * @param movieId 조회할 영화의 ID
     * @return 배우 목록이 포함된 영화 레코드, 존재하지 않으면 null
     */
    @GetMapping("/{movieId}")
    fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorRecord? {
        return movieRepository.getMovieWithActors(movieId)
    }

    /**
     * 각 영화별 출연 배우 수를 반환합니다.
     *
     * @return 영화별 배우 수 정보 목록
     */
    @GetMapping("/count")
    fun getMovieActorsCount(): List<MovieActorCountRecord> {
        return movieRepository.getMovieActorsCount()
    }

    /**
     * 제작자가 직접 출연한 영화 목록을 반환합니다.
     *
     * @return 제작자 겸 배우 정보가 포함된 영화 레코드 목록
     */
    @GetMapping("/acting-producers")
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorRecord> {
        return movieRepository.findMoviesWithActingProducers()
    }
}
