package exposed.multitenant.springweb.controller

import exposed.multitenant.springweb.domain.dtos.ActorDTO
import exposed.multitenant.springweb.domain.repository.ActorExposedRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/actors")
class ActorController(
    private val actorRepository: ActorExposedRepository,
) {


    @GetMapping
    @Transactional(readOnly = true)
    fun getAllActors(): List<ActorDTO> {
        return actorRepository.findAll()
    }
}
