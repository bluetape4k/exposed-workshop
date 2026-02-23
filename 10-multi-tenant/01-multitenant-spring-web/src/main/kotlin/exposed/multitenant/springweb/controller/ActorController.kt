package exposed.multitenant.springweb.controller

import exposed.multitenant.springweb.domain.dtos.ActorRecord
import exposed.multitenant.springweb.domain.repository.ActorExposedRepository
import io.bluetape4k.logging.KLogging
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 배우 조회 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/actors")
@Transactional(readOnly = true)
class ActorController(
    private val actorRepository: ActorExposedRepository,
) {

    companion object: KLogging()

    @GetMapping
    fun getAllActors(): List<ActorRecord> {
        return actorRepository.findAll()
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ActorRecord? {
        return actorRepository.findByIdOrNull(id)
    }
}
