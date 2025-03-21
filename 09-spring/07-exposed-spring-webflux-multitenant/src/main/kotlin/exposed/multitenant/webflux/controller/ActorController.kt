package exposed.multitenant.webflux.controller

import exposed.multitenant.webflux.domain.dtos.ActorDTO
import exposed.multitenant.webflux.domain.model.toActorDTO
import exposed.multitenant.webflux.domain.repository.ActorExposedRepository
import exposed.multitenant.webflux.tenant.newSuspendedTransactionWithCurrentReactorTenant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/actors")
class ActorController(
    private val actorRepository: ActorExposedRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO) {

    @GetMapping
    suspend fun getAllActors(): List<ActorDTO> =
        newSuspendedTransactionWithCurrentReactorTenant {
            actorRepository.findAll().map { it.toActorDTO() }
        }
}
