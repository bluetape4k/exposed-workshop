package exposed.workshop.springwebflux.controller

import exposed.workshop.springwebflux.domain.model.ActorRecord
import exposed.workshop.springwebflux.domain.model.toActorRecord
import exposed.workshop.springwebflux.domain.repository.ActorRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("DEPRECATION")
@RestController
@RequestMapping("/actors")
class ActorController(
    private val actorRepository: ActorRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO) {

    companion object: KLoggingChannel()

    @GetMapping("/{id}")
    suspend fun getActorById(@PathVariable("id") actorId: Long): ActorRecord? {
        return newSuspendedTransaction(readOnly = true) {
            log.debug { "current transaction=$this" }
            actorRepository.findById(actorId)?.toActorRecord()
        }
    }

    @GetMapping
    suspend fun searchActors(request: ServerHttpRequest): List<ActorRecord> {
        val params = request.queryParams.map { it.key to it.value.first() }.toMap()
        return when {
            params.isEmpty() -> newSuspendedTransaction(readOnly = true) {
                actorRepository.findAll().map { it.toActorRecord() }
            }
            else -> newSuspendedTransaction(readOnly = true) {
                actorRepository.searchActor(params)
            }
        }
    }

    @PostMapping
    suspend fun createActor(@RequestBody actor: ActorRecord): ActorRecord =
        newSuspendedTransaction {
            actorRepository.create(actor).toActorRecord()
        }

    @DeleteMapping("/{id}")
    suspend fun deleteActor(@PathVariable("id") actorId: Long): Int =
        newSuspendedTransaction {
            actorRepository.deleteById(actorId)
        }
}
