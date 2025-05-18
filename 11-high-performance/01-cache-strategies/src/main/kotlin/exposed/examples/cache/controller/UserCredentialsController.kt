package exposed.examples.cache.controller

import exposed.examples.cache.domain.model.UserCredentialsDTO
import exposed.examples.cache.domain.repository.UserCredentialsCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user-credentials")
class UserCredentialsController(private val repository: UserCredentialsCacheRepository) {

    companion object: KLoggingChannel()

    @GetMapping
    fun findAll(@RequestParam(name = "limit") limit: Int? = null): List<UserCredentialsDTO> {
        log.debug { "Finding all user credentials with limit: $limit" }
        return transaction {
            repository.findAll(limit = limit)
        }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable(name = "id") id: String): UserCredentialsDTO? {
        log.debug { "Getting user credentials with id: $id" }
        return transaction {
            repository.get(id)
        }
    }

    @GetMapping("/all")
    fun getAll(@RequestParam(name = "ids") ids: List<String>): List<UserCredentialsDTO> {
        log.debug { "Getting all user credentials with ids: $ids" }
        return transaction {
            repository.getAll(ids)
        }
    }

    @DeleteMapping("/invalidate")
    fun invalidate(@RequestParam(name = "ids") ids: List<String>): Long {
        if (ids.isEmpty()) {
            return 0
        }
        log.debug { "Invalidating cache for ids: $ids" }
        return repository.invalidate(*ids.toTypedArray())
    }

    @DeleteMapping("/invalidate/all")
    fun invalidateAll() {
        repository.invalidateAll()
    }

    @DeleteMapping("/invalidate/pattern")
    fun invalidatePattern(@RequestParam(name = "pattern") pattern: String): Long {
        if (pattern.isEmpty()) {
            return 0
        }
        log.debug { "Invalidating cache for pattern: $pattern" }
        return repository.invalidateByPattern(pattern)
    }

}
