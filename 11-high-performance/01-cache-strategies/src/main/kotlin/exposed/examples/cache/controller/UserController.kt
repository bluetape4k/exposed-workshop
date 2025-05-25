package exposed.examples.cache.controller

import exposed.examples.cache.domain.model.UserDTO
import exposed.examples.cache.domain.repository.UserCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(private val repository: UserCacheRepository) {

    companion object: KLoggingChannel()

    @GetMapping
    fun findAll(@RequestParam(name = "limit") limit: Int? = null): List<UserDTO> {
        log.debug { "Finding all users with limit: $limit" }
        return transaction {
            repository.findAll(limit = limit, where = { Op.TRUE })
        }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable(name = "id") id: Long): UserDTO? {
        log.debug { "Getting user with id: $id" }
        return transaction {
            repository.get(id)
        }
    }

    @GetMapping("/all")
    fun getAll(@RequestParam(name = "ids") ids: List<Long>): List<UserDTO> {
        log.debug { "Getting all users with ids: $ids" }
        return transaction {
            repository.getAll(ids)
        }
    }

    @DeleteMapping("/invalidate")
    fun invalidate(@RequestParam(name = "ids") ids: List<Long>): Long {
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
    fun invalidateByPattern(@RequestParam(name = "patterns") pattern: String): Long {
        return repository.invalidateByPattern(pattern)
    }

    @PostMapping
    fun put(@RequestBody userDTO: UserDTO): UserDTO {
        log.debug { "Updating user with id: ${userDTO.id}" }
        repository.put(userDTO)
        return userDTO
    }
}
