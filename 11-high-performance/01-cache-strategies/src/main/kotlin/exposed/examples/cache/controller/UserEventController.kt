package exposed.examples.cache.controller

import exposed.examples.cache.domain.model.UserEventDTO
import exposed.examples.cache.domain.repository.UserEventCacheRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user-events")
class UserEventController(
    private val repository: UserEventCacheRepository,
) {

    companion object: KLogging()

    @PostMapping
    fun insert(@RequestBody userEvent: UserEventDTO): Boolean {
        log.debug { "Inserting user event: $userEvent" }
        return repository.put(userEvent)
    }

    @PostMapping("/bulk")
    fun insertBulk(@RequestBody userEvents: List<UserEventDTO>): Boolean {
        log.debug { "Inserting user events: $userEvents" }
        repository.putAll(userEvents)
        return true
    }
}
