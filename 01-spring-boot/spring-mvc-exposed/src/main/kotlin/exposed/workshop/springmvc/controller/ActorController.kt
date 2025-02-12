package exposed.workshop.springmvc.controller

import exposed.workshop.springmvc.domain.ActorDTO
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

@RestController
@Transactional(readOnly = true)
@RequestMapping("/actors")
class ActorController(private val actorRepository: ActorRepository) {

    companion object: KLogging()

    @GetMapping("/{id}")
    fun getActorById(@PathVariable("id") actorId: Long): ActorDTO? {
        return actorRepository.findById(actorId)
    }

    @GetMapping
    fun searchActors(request: HttpServletRequest): List<ActorDTO> {
        val params = request.parameterMap.map { it.key to it.value.firstOrNull() }.toMap()
        log.debug { "Search Actors... params=$params" }

        return actorRepository.searchActors(params)
    }

    @PostMapping
    @Transactional
    fun createActor(@RequestBody actor: ActorDTO): ActorDTO {
        return actorRepository.create(actor)
    }

    @DeleteMapping("/{id}")
    @Transactional
    fun deleteActorById(@PathVariable("id") actorId: Long): Int {
        return actorRepository.deleteById(actorId)
    }
}
