package exposed.multitenant.webflux.controller

import exposed.multitenant.webflux.domain.model.ActorRecord
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

/**
 * WebFlux 환경에서 배우 조회 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/actors")
class ActorController(
    private val actorRepository: ActorExposedRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel()

    @GetMapping
    suspend fun getAllActors(): List<ActorRecord> = newSuspendedTransactionWithCurrentReactorTenant {
        actorRepository.findAll()
    }

    @GetMapping("/{id}")
    suspend fun findById(@PathVariable("id") id: Long): ActorRecord? =
        newSuspendedTransactionWithCurrentReactorTenant {
            actorRepository.findByIdOrNull(id)
        }
}
