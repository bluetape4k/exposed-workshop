package exposed.examples.springmvc.controller

import exposed.examples.springmvc.domain.dtos.ActorDTO
import exposed.examples.springmvc.domain.dtos.toActorDTO
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

@RestController
@Transactional(readOnly = true)
@RequestMapping("/actors")
class ActorController(private val actorRepository: ActorExposedRepository) {

    companion object: KLogging()

    @GetMapping("/{id}")
    fun getActorById(@PathVariable("id") actorId: Long): ActorDTO? {
        return actorRepository.findByIdOrNull(actorId)?.toActorDTO()
    }

    @GetMapping
    fun searchActors(request: HttpServletRequest): List<ActorDTO> {
        val params = request.parameterMap.map { it.key to it.value.firstOrNull() }.toMap()
        log.debug { "Search Actors... params=$params" }

        return actorRepository.searchActors(params).map { it.toActorDTO() }
    }

    @PostMapping
    @Transactional
    fun createActor(@RequestBody actor: ActorDTO): ActorDTO {
        return actorRepository.create(actor).toActorDTO()
    }

    @DeleteMapping("/{id}")
    @Transactional
    fun deleteById(@PathVariable("id") actorId: Long): Int {
        return actorRepository.deleteById(actorId)
    }
}
