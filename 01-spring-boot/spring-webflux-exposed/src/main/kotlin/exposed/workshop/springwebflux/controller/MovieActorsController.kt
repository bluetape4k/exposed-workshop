package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.model.MovieActorCountRecord
import exposed.workshop.springwebflux.domain.model.MovieWithActorRecord
import exposed.workshop.springwebflux.domain.model.MovieWithProducingActorRecord
import exposed.workshop.springwebflux.domain.repository.MovieRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 영화와 배우 관계에 대한 코루틴 기반 REST API 컨트롤러.
 *
 * `/movie-actors` 경로로 영화별 출연 배우 조회, 카운트, 제작자 겸 배우 정보를 제공합니다.
 */
@Suppress("DEPRECATION")
@RestController
@RequestMapping("/movie-actors")
class MovieActorsController(
    private val movieRepository: MovieRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO) {

    companion object: KLoggingChannel()

    /**
     * 특정 영화와 해당 영화에 출연한 배우 목록을 조회합니다.
     *
     * @param movieId 조회할 영화 ID
     * @return 영화 및 배우 정보, 존재하지 않으면 null
     */
    @GetMapping("/{movieId}")
    suspend fun getMovieWithActors(@PathVariable movieId: Long): MovieWithActorRecord? =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.getMovieWithActors(movieId)
        }

    /**
     * 각 영화별 출연 배우 수를 조회합니다.
     *
     * @return 영화명과 배우 수를 담은 목록
     */
    @GetMapping("/count")
    suspend fun getMovieActorsCount(): List<MovieActorCountRecord> =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.getMovieActorsCount()
        }

    /**
     * 제작자가 직접 배우로 출연한 영화 목록을 조회합니다.
     *
     * @return 영화명과 제작자 겸 배우 이름을 담은 목록
     */
    @GetMapping("/acting-producers")
    suspend fun findMoviesWithActingProducers(): List<MovieWithProducingActorRecord> =
        newSuspendedTransaction(readOnly = true) {
            movieRepository.findMoviesWithActingProducers().toList()
        }
}
