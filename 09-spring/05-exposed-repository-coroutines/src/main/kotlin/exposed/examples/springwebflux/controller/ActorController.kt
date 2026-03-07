package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.domain.model.ActorRecord
import exposed.examples.springwebflux.domain.repository.ActorExposedRepository
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
 * 배우(Actor) 정보를 관리하는 코루틴 기반 REST 컨트롤러.
 *
 * Spring WebFlux 환경에서 코루틴 suspend 함수를 사용하여 비동기적으로
 * 배우 조회, 검색, 생성, 삭제 기능을 HTTP 엔드포인트로 제공합니다.
 * [newSuspendedTransaction]을 통해 Exposed 트랜잭션을 코루틴과 통합합니다.
 *
 * @property actorRepository 배우 데이터 접근을 위한 Exposed 리포지토리
 */
@Suppress("DEPRECATION")
@RestController
@RequestMapping("/actors")
class ActorController(
    private val actorRepository: ActorExposedRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel()

    /**
     * ID로 배우를 조회합니다.
     *
     * @param actorId 조회할 배우의 ID
     * @return 조회된 배우 레코드, 존재하지 않으면 null
     */
    @GetMapping("/{id}")
    suspend fun getActorById(@PathVariable("id") actorId: Long): ActorRecord? =
        newSuspendedTransaction(readOnly = true) {
            actorRepository.findByIdOrNull(actorId)
        }

    /**
     * 쿼리 파라미터를 기반으로 배우를 검색합니다.
     *
     * @param request 검색 조건이 담긴 서버 HTTP 요청
     * @return 검색 조건에 맞는 배우 레코드 목록, 파라미터가 없으면 빈 목록
     */
    @GetMapping
    suspend fun searchActors(request: ServerHttpRequest): List<ActorRecord> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()

        return when {
            params.isEmpty() -> emptyList()
            else -> newSuspendedTransaction(readOnly = true) {
                actorRepository.searchActor(params)
            }
        }
    }

    /**
     * 새로운 배우를 생성합니다.
     *
     * @param actor 생성할 배우 정보
     * @return 생성된 배우 레코드 (ID 포함)
     */
    @PostMapping
    suspend fun createActor(@RequestBody actor: ActorRecord): ActorRecord =
        newSuspendedTransaction {
            actorRepository.create(actor)
        }

    /**
     * ID로 배우를 삭제합니다.
     *
     * @param actorId 삭제할 배우의 ID
     * @return 삭제된 레코드 수
     */
    @DeleteMapping("/{id}")
    suspend fun deleteActor(@PathVariable("id") actorId: Long): Int =
        newSuspendedTransaction {
            actorRepository.deleteById(actorId)
        }

}
