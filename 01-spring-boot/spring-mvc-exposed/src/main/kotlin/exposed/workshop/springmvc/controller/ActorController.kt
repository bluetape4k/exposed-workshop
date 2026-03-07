package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.domain.model.ActorRecord
import exposed.workshop.springmvc.domain.repository.ActorRepository
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
 * 배우(Actor) 관련 REST API 컨트롤러.
 *
 * `/actors` 경로로 배우 정보의 조회, 생성, 삭제 기능을 제공합니다.
 * 기본적으로 읽기 전용 트랜잭션으로 동작하며, 쓰기 작업은 별도로 트랜잭션을 지정합니다.
 *
 * @param actorRepository 배우 데이터 접근 레포지토리
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping("/actors")
class ActorController(private val actorRepository: ActorRepository) {

    companion object: KLogging()

    /**
     * ID로 배우를 조회합니다.
     *
     * HTTP GET `/actors/{id}`
     *
     * @param actorId 조회할 배우의 고유 식별자
     * @return 해당 ID의 [ActorRecord], 존재하지 않으면 null
     */
    @GetMapping("/{id}")
    fun getActorById(@PathVariable("id") actorId: Long): ActorRecord? {
        return actorRepository.findById(actorId)
    }

    /**
     * 쿼리 파라미터를 기반으로 배우를 검색합니다.
     *
     * HTTP GET `/actors?firstName=...&lastName=...&birthday=...`
     *
     * 지원 필터: `id`, `firstName`, `lastName`, `birthday`
     *
     * @param request HTTP 서블릿 요청 (쿼리 파라미터 포함)
     * @return 조건에 맞는 [ActorRecord] 목록
     */
    @GetMapping
    fun searchActors(request: HttpServletRequest): List<ActorRecord> {
        val params = request.parameterMap.map { it.key to it.value.firstOrNull() }.toMap()
        log.debug { "Search Actors... params=$params" }

        return actorRepository.searchActors(params)
    }

    /**
     * 새로운 배우를 생성합니다.
     *
     * HTTP POST `/actors`
     *
     * @param actor 생성할 배우 정보가 담긴 [ActorRecord] (id는 무시됨)
     * @return 생성된 배우의 [ActorRecord] (서버에서 할당된 id 포함)
     */
    @PostMapping
    @Transactional
    fun createActor(@RequestBody actor: ActorRecord): ActorRecord {
        return actorRepository.create(actor)
    }

    /**
     * ID로 배우를 삭제합니다.
     *
     * HTTP DELETE `/actors/{id}`
     *
     * @param actorId 삭제할 배우의 고유 식별자
     * @return 삭제된 행 수 (성공 시 1, 존재하지 않으면 0)
     */
    @DeleteMapping("/{id}")
    @Transactional
    fun deleteActorById(@PathVariable("id") actorId: Long): Int {
        return actorRepository.deleteById(actorId)
    }
}
