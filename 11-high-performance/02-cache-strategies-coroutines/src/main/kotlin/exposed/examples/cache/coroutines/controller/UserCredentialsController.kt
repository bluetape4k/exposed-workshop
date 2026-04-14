package exposed.examples.cache.coroutines.controller

import exposed.examples.cache.coroutines.domain.model.UserCredentialsRecord
import exposed.examples.cache.coroutines.domain.repository.UserCredentialsCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Suppress("DEPRECATION")
@RestController
@RequestMapping("/user-credentials")
/**
 * suspend 기반 Read-Only 캐시 컨트롤러입니다.
 */
class UserCredentialsController(private val repository: UserCredentialsCacheRepository) {

    companion object: KLoggingChannel()

    @GetMapping
    suspend fun findAll(@RequestParam(name = "limit") limit: Int? = null): List<UserCredentialsRecord> {
        log.debug { "Finding all user credentials with limit: $limit" }
        return repository.findAll(limit = limit)
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable(name = "id") id: String): UserCredentialsRecord? {
        log.debug { "Getting user credentials with id: $id" }
        return repository.get(id)
    }

    @GetMapping("/all")
    suspend fun getAll(@RequestParam(name = "ids") ids: List<String>): List<UserCredentialsRecord> {
        log.debug { "Getting all user credentials with ids: $ids" }
        return repository.getAll(ids).map { it.value }
    }

    @DeleteMapping("/invalidate")
    suspend fun invalidate(@RequestParam(name = "ids") ids: List<String>): Long {
        if (ids.isEmpty()) {
            return 0
        }
        log.debug { "Invalidating cache for ids: $ids" }
        repository.invalidateAll(ids)
        return ids.size.toLong()
    }

    @DeleteMapping("/invalidate/all")
    suspend fun invalidateAll() {
        log.debug { "Invalidating all user credentials cache" }
        repository.clear()
    }

    @DeleteMapping("/invalidate/pattern")
    suspend fun invalidatePattern(@RequestParam(name = "pattern") pattern: String): Long {
        if (pattern.isEmpty()) {
            return 0
        }
        log.debug { "Invalidating cache for pattern: $pattern" }
        return repository.invalidateByPattern(pattern)
    }
}
