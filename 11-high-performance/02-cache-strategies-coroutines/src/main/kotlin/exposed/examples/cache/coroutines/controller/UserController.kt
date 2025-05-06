package exposed.examples.cache.coroutines.controller

import exposed.examples.cache.coroutines.domain.model.UserDTO
import exposed.examples.cache.coroutines.domain.repository.UserCacheRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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

    companion object: KLogging()

    @GetMapping
    suspend fun findAll(@RequestParam(name = "limit") limit: Int? = null): List<UserDTO> {
        log.debug { "Finding all users with limit: $limit" }
        return newSuspendedTransaction(readOnly = true) {
            repository.findAll(limit = limit, where = { Op.TRUE })
        }
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable(name = "id") id: Long): UserDTO? {
        log.debug { "Getting user with id: $id" }
        return newSuspendedTransaction(readOnly = true) {
            repository.get(id)
        }
    }

    @GetMapping("/all")
    suspend fun getAll(@RequestParam(name = "ids") ids: List<Long>): List<UserDTO> {
        log.debug { "Getting all users with ids: $ids" }
        return newSuspendedTransaction(readOnly = true) {
            repository.getAll(ids)
        }
    }

    @DeleteMapping("/invalidate")
    suspend fun invalidate(@RequestParam(name = "ids") ids: List<Long>): Long {
        if (ids.isEmpty()) {
            return 0
        }
        log.debug { "Invalidating cache for ids: $ids" }
        return newSuspendedTransaction {
            repository.invalidate(*ids.toTypedArray())
        }
    }

    @DeleteMapping("/invalidate/all")
    suspend fun invalidateAll() {
        newSuspendedTransaction {
            repository.invalidateAll()
        }
    }

    @DeleteMapping("/invalidate/pattern")
    suspend fun invalidateByPattern(@RequestParam(name = "patterns") pattern: String): Long {
        return newSuspendedTransaction {
            repository.invalidateByPattern(pattern)
        }
    }

    @PostMapping
    suspend fun put(@RequestBody userDTO: UserDTO): UserDTO {
        log.debug { "Updating user with id: ${userDTO.id}" }
        newSuspendedTransaction { repository.put(userDTO) }
        return userDTO
    }
}
