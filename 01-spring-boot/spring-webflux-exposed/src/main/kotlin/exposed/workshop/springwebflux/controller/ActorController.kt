package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.model.ActorRecord
import exposed.workshop.springwebflux.domain.model.toActorRecord
import exposed.workshop.springwebflux.domain.repository.ActorRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * 배우(Actor) 관련 코루틴 기반 REST API 컨트롤러.
 *
 * `/actors` 경로로 배우 조회, 검색, 생성, 삭제 기능을 Suspended 트랜잭션으로 제공합니다.
 */
@Suppress("DEPRECATION")
@RestController
@RequestMapping("/actors")
class ActorController(
    private val actorRepository: ActorRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO) {

    companion object: KLoggingChannel()

    /**
     * ID로 배우를 조회합니다.
     *
     * @param actorId 조회할 배우 ID
     * @return 배우 레코드, 존재하지 않으면 null
     */
    @GetMapping("/{id}")
    suspend fun getActorById(@PathVariable("id") actorId: Long): ActorRecord? {
        return newSuspendedTransaction(readOnly = true) {
            log.debug { "current transaction=$this" }
            actorRepository.findById(actorId)?.toActorRecord()
        }
    }

    /**
     * 요청 파라미터를 기반으로 배우를 검색합니다. 파라미터가 없으면 전체를 반환합니다.
     *
     * @param request 검색 조건이 포함된 HTTP 요청
     * @return 조건에 맞는 배우 레코드 목록
     */
    @GetMapping
    suspend fun searchActors(request: ServerHttpRequest): List<ActorRecord> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()
        return when {
            params.isEmpty() -> newSuspendedTransaction(readOnly = true) {
                actorRepository.findAll().map { it.toActorRecord() }
            }
            else -> newSuspendedTransaction(readOnly = true) {
                actorRepository.searchActor(params)
            }
        }
    }

    /**
     * 새 배우를 생성합니다.
     *
     * @param actor 생성할 배우 정보
     * @return 생성된 배우 레코드 (ID 포함)
     */
    @PostMapping
    suspend fun createActor(@RequestBody actor: ActorRecord): ActorRecord =
        newSuspendedTransaction {
            actorRepository.create(actor).toActorRecord()
        }

    /**
     * ID로 배우를 삭제합니다.
     *
     * @param actorId 삭제할 배우 ID
     * @return 삭제된 행 수
     */
    @DeleteMapping("/{id}")
    suspend fun deleteActor(@PathVariable("id") actorId: Long): Int =
        newSuspendedTransaction {
            actorRepository.deleteById(actorId)
        }
}
