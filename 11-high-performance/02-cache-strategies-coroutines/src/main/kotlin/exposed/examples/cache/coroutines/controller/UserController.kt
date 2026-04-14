package exposed.examples.cache.coroutines.controller

import exposed.examples.cache.coroutines.domain.model.UserRecord
import exposed.examples.cache.coroutines.domain.repository.UserCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.Op
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * suspend 기반 캐시 API 엔드포인트 (Read/Write Through)입니다.
 */
@Suppress("DEPRECATION")
@RestController
@RequestMapping("/users")
class UserController(private val repository: UserCacheRepository) {

    companion object: KLoggingChannel()

    @GetMapping
    suspend fun findAll(@RequestParam(name = "limit") limit: Int? = null): List<UserRecord> {
        log.debug { "Finding all users with limit: $limit" }
        return repository.findAll(limit = limit, where = { Op.TRUE })
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable(name = "id") id: Long): UserRecord? {
        log.debug { "Getting user with id: $id" }
        return repository.get(id)
    }

    @GetMapping("/all")
    suspend fun getAll(@RequestParam(name = "ids") ids: List<Long>): List<UserRecord> {
        log.debug { "Getting all users with ids: $ids" }
        return repository.getAll(ids).map { it.value }
    }

    @DeleteMapping("/invalidate")
    suspend fun invalidate(@RequestParam(name = "ids") ids: List<Long>): Long {
        if (ids.isEmpty()) {
            return 0
        }
        log.debug { "Invalidating cache for ids: $ids" }
        repository.invalidateAll(ids)
        return ids.size.toLong()
    }

    @DeleteMapping("/invalidate/all")
    suspend fun invalidateAll() {
        repository.clear()
    }

    @DeleteMapping("/invalidate/pattern")
    suspend fun invalidateByPattern(@RequestParam(name = "patterns") pattern: String): Long {
        return repository.invalidateByPattern(pattern)
    }

    @PostMapping
    suspend fun put(@RequestBody userRecord: UserRecord): UserRecord {
        log.debug { "Updating user with id: ${userRecord.id}" }
        repository.put(userRecord.id, userRecord)
        return userRecord
    }
}
