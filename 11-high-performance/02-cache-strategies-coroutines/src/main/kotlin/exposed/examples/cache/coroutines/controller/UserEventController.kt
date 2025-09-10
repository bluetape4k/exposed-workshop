package exposed.examples.cache.coroutines.controller

import exposed.examples.cache.coroutines.domain.model.UserEventDTO
import exposed.examples.cache.coroutines.domain.repository.UserEventCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("DEPRECATION")
@RestController
@RequestMapping("/user-events")
class UserEventController(
    private val repository: UserEventCacheRepository,
) {
    companion object: KLoggingChannel()

    @PostMapping
    suspend fun insert(@RequestBody userEvent: UserEventDTO): Boolean {
        log.debug { "Inserting user event: $userEvent" }
        return repository.put(userEvent)
    }

    @PostMapping("/bulk")
    suspend fun insertBulk(@RequestBody userEvents: List<UserEventDTO>): Boolean {
        log.debug { "Inserting user events: $userEvents" }
        repository.putAll(userEvents)
        return true
    }
}
