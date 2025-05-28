package exposed.examples.cache.coroutines.domain.model

import exposed.examples.cache.coroutines.utils.faker
import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

enum class UserEventType {
    LOGIN,
    LOGOUT,
    SIGNUP,
    PASSWORD_RESET,
    ACCOUNT_UPDATE,
    ACCOUNT_DELETION,
    EMAIL_VERIFICATION,
    TWO_FACTOR_AUTHENTICATION,
    PASSWORD_CHANGE,
    PROFILE_UPDATE,
    USERNAME_CHANGE,
    EMAIL_CHANGE,
    PHONE_NUMBER_CHANGE,
}

/**
 * 사용자 이벤트를 Write-Behind 로 캐시 -> DB에 저장합니다.
 */
object UserEventTable: LongIdTable("user_action") {
    val username = varchar("username", 255)

    val eventSource = varchar("event_source", 128)
    val eventType = enumerationByName<UserEventType>("event_type", 32)
    val eventDetails = varchar("event_details", 2000).nullable()
    val eventTime = timestamp("event_time")

    init {
        index("idx_user_events", isUnique = false, username, eventTime)
    }
}

class UserEventEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<UserEventEntity>(UserEventTable)

    var username by UserEventTable.username
    var eventSource by UserEventTable.eventSource
    var eventType by UserEventTable.eventType
    var eventDetails by UserEventTable.eventDetails
    var eventTime by UserEventTable.eventTime

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = toStringBuilder()
        .add("username", username)
        .add("eventSource", eventSource)
        .add("eventType", eventType)
        .add("eventDetails", eventDetails)
        .add("eventTime", eventTime)
        .toString()
}

data class UserEventDTO(
    override val id: Long = Snowflakers.Global.nextId(),
    val username: String,
    val eventSource: String,
    val eventType: UserEventType,
    val eventDetails: String?,
    val eventTime: Instant,
): HasIdentifier<Long>

fun ResultRow.toUserEventDTO() = UserEventDTO(
    id = this[UserEventTable.id].value,
    username = this[UserEventTable.username],
    eventSource = this[UserEventTable.eventSource],
    eventType = this[UserEventTable.eventType],
    eventDetails = this[UserEventTable.eventDetails],
    eventTime = this[UserEventTable.eventTime],
)

fun newUserEventDTO() = UserEventDTO(
    username = faker.internet().username(),
    eventSource = faker.app().name(),
    eventType = UserEventType.entries.random(),
    eventDetails = faker.lorem().sentence(),
    eventTime = Instant.now()
)
