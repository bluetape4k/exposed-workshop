package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.ActorDTO
import exposed.workshop.springwebflux.domain.repository.ActorRepository
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
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
    private val actorRepository: ActorRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLogging()

    @GetMapping("/{id}")
    suspend fun getActorById(@PathVariable("id") actorId: Long): ActorDTO? {
        return actorRepository.findById(actorId)
    }

    @GetMapping
    suspend fun searchActors(request: ServerHttpRequest): List<ActorDTO> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()
        return when {
            params.isEmpty() -> emptyList()
            else -> actorRepository.searchActor(params).toList()
        }
    }

    @PostMapping
    suspend fun createActor(@RequestBody actor: ActorDTO): ActorDTO {
        return actorRepository.create(actor)
    }

    @DeleteMapping("/{id}")
    suspend fun deleteActor(@PathVariable("id") actorId: Long): Int {
        return actorRepository.deleteById(actorId)
    }
}
