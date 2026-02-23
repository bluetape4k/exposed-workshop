package exposed.examples.cache.controller

import exposed.examples.cache.domain.model.UserEventRecord
import exposed.examples.cache.domain.repository.UserEventCacheRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Write Behind 캐시 이벤트 저장 API입니다.
 */
@RestController
@RequestMapping("/user-events")
class UserEventController(
    private val repository: UserEventCacheRepository,
) {
    companion object: KLoggingChannel()

    @PostMapping
    fun insert(@RequestBody userEvent: UserEventRecord): Boolean {
        log.debug { "Inserting user event: $userEvent" }
        return repository.put(userEvent)
    }

    @PostMapping("/bulk")
    fun insertBulk(@RequestBody userEvents: List<UserEventRecord>): Boolean {
        log.debug { "Inserting bulk user events. size=${userEvents.size}" }
        repository.putAll(userEvents)
        return true
    }
}
