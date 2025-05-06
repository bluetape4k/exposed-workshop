package exposed.examples.cache.coroutines.controller

import exposed.examples.cache.coroutines.domain.model.UserEventDTO
import exposed.examples.cache.coroutines.domain.repository.UserEventCacheRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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
    suspend fun insert(@RequestBody userEvent: UserEventDTO): Boolean {
        log.debug { "Inserting user event: $userEvent" }
        return newSuspendedTransaction {
            repository.put(userEvent)
        }
    }

    @PostMapping("/bulk")
    suspend fun insertBulk(@RequestBody userEvents: List<UserEventDTO>): Boolean {
        log.debug { "Inserting user events: $userEvents" }
        newSuspendedTransaction {
            repository.putAll(userEvents)
        }
        return true
    }
}
