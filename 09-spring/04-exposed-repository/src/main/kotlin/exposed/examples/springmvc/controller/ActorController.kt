package exposed.examples.springmvc.controller

import exposed.examples.springmvc.domain.model.ActorRecord
import exposed.examples.springmvc.domain.repository.ActorExposedRepository
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
 * 배우(Actor) 정보를 관리하는 REST 컨트롤러.
 *
 * 배우 조회, 검색, 생성, 삭제 기능을 HTTP 엔드포인트로 제공합니다.
 * 기본적으로 읽기 전용 트랜잭션을 사용하며, 쓰기 작업은 별도의 트랜잭션을 적용합니다.
 *
 * @property actorRepository 배우 데이터 접근을 위한 Exposed 리포지토리
 */
@RestController
@Transactional(readOnly = true)
@RequestMapping("/actors")
class ActorController(private val actorRepository: ActorExposedRepository) {

    companion object: KLogging()

    /**
     * ID로 배우를 조회합니다.
     *
     * @param actorId 조회할 배우의 ID
     * @return 조회된 배우 레코드, 존재하지 않으면 null
     */
    @GetMapping("/{id}")
    fun getActorById(@PathVariable("id") actorId: Long): ActorRecord? {
        return actorRepository.findByIdOrNull(actorId)
    }

    /**
     * 쿼리 파라미터를 기반으로 배우를 검색합니다.
     *
     * @param request 검색 조건이 담긴 HTTP 요청
     * @return 검색 조건에 맞는 배우 레코드 목록
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
     * @param actor 생성할 배우 정보
     * @return 생성된 배우 레코드 (ID 포함)
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
     * @return 삭제된 레코드 수
     */
    @DeleteMapping("/{id}")
    @Transactional
    fun deleteById(@PathVariable("id") actorId: Long): Int {
        return actorRepository.deleteById(actorId)
    }
}
