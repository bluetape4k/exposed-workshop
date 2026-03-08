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
 * 영화와 배우 관계에 대한 REST API 컨트롤러.
 *
 * `/movie-actors` 경로로 영화별 출연 배우 조회, 카운트, 제작자 겸 배우 정보를 제공합니다.
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping("/movie-actors")
class MovieActorsController(private val movieRepo: MovieRepository) {

    companion object: KLogging()

    /**
     * 특정 영화와 해당 영화에 출연한 배우 목록을 조회합니다.
     *
     * @param movieId 조회할 영화 ID
     * @return 영화 및 배우 정보, 존재하지 않으면 null
     */
    @GetMapping("/{movieId}")
    fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorRecord? {
        return movieRepo.getMovieWithActors(movieId)
    }

    /**
     * 각 영화별 출연 배우 수를 조회합니다.
     *
     * @return 영화명과 배우 수를 담은 목록
     */
    @GetMapping("/count")
    fun getMovieActorsCount(): List<MovieActorCountRecord> {
        return movieRepo.getMovieActorsCount()
    }

    /**
     * 제작자가 직접 배우로 출연한 영화 목록을 조회합니다.
     *
     * @return 영화명과 제작자 겸 배우 이름을 담은 목록
     */
    @GetMapping("/acting-producers")
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorRecord> {
        return movieRepo.findMoviesWithActingProducers()
    }
}
