package exposed.multitenant.webflux.controller

import exposed.multitenant.webflux.domain.dtos.ActorDTO
import exposed.multitenant.webflux.domain.repository.ActorExposedRepository
import exposed.multitenant.webflux.tenant.newSuspendedTransactionWithCurrentReactorTenant
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/actors")
class ActorController(
    private val actorRepository: ActorExposedRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel()

    @GetMapping
    suspend fun getAllActors(): List<ActorDTO> = newSuspendedTransactionWithCurrentReactorTenant {
        actorRepository.findAll()
    }

    @GetMapping("/{id}")
    suspend fun findById(@PathVariable("id") id: Long): ActorDTO? =
        newSuspendedTransactionWithCurrentReactorTenant {
            actorRepository.findByIdOrNull(id)
        }
}
