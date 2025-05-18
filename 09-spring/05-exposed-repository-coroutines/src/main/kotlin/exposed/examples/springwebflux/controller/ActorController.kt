package exposed.examples.springwebflux.controller

import exposed.examples.springwebflux.domain.dtos.ActorDTO
import exposed.examples.springwebflux.domain.model.toActorDTO
import exposed.examples.springwebflux.domain.repository.ActorExposedRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/actors")
class ActorController(
    private val actorRepository: ActorExposedRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel()

    @GetMapping("/{id}")
    suspend fun getActorById(@PathVariable("id") actorId: Long): ActorDTO? =
        newSuspendedTransaction(readOnly = true) {
            actorRepository.findByIdOrNull(actorId)?.toActorDTO()
        }

    @GetMapping
    suspend fun searchActors(request: ServerHttpRequest): List<ActorDTO> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()

        return when {
            params.isEmpty() -> emptyList()
            else -> newSuspendedTransaction(readOnly = true) {
                actorRepository.searchActor(params).map { it.toActorDTO() }
            }
        }
    }

    @PostMapping
    suspend fun createActor(@RequestBody actor: ActorDTO): ActorDTO =
        newSuspendedTransaction {
            actorRepository.create(actor).toActorDTO()
        }

    @DeleteMapping("/{id}")
    suspend fun deleteActor(@PathVariable("id") actorId: Long): Int =
        newSuspendedTransaction {
            actorRepository.deleteById(actorId)
        }

}
