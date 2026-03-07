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
 * `/actors` 경로로 배우 조회, 검색, 생성, 삭제 기능을 제공합니다.
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping("/actors")
class ActorController(private val actorRepository: ActorRepository) {

    companion object: KLogging()

    /**
     * ID로 배우를 조회합니다.
     *
     * @param actorId 조회할 배우의 ID
     * @return 배우 정보, 존재하지 않으면 null
     */
    @GetMapping("/{id}")
    fun getActorById(@PathVariable("id") actorId: Long): ActorRecord? {
        return actorRepository.findById(actorId)
    }

    /**
     * 요청 파라미터를 기반으로 배우를 검색합니다.
     *
     * @param request 검색 조건이 포함된 HTTP 요청
     * @return 조건에 맞는 배우 목록
     */
    @GetMapping
    fun searchActors(request: HttpServletRequest): List<ActorRecord> {
        val params = request.parameterMap.map { it.key to it.value.firstOrNull() }.toMap()
        log.debug { "Search Actors... params=$params" }

        return actorRepository.searchActors(params)
    }

    /**
     * 새 배우를 생성합니다.
     *
     * @param actor 생성할 배우 정보
     * @return 생성된 배우 정보 (ID 포함)
     */
    @PostMapping
    @Transactional
    fun createActor(@RequestBody actor: ActorRecord): ActorRecord {
        return actorRepository.create(actor)
    }

    /**
     * ID로 배우를 삭제합니다.
     *
     * @param actorId 삭제할 배우의 ID
     * @return 삭제된 행 수
     */
    @DeleteMapping("/{id}")
    @Transactional
    fun deleteActorById(@PathVariable("id") actorId: Long): Int {
        return actorRepository.deleteById(actorId)
    }
}
