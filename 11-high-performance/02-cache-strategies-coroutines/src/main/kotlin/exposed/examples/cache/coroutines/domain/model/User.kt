package exposed.examples.cache.coroutines.domain.model

import exposed.examples.cache.coroutines.utils.faker
import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import net.datafaker.Faker
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate

/**
 * Read Through 를 통해 DB에서 읽어온 사용자 정보를 캐시에 저장합니다.
 * Write Through 를 통해 DB에 사용자 정보를 저장합니다.
 * 캐시 Invalidate 시에 DB에 영향을 주지 않도록 해야 합니다.
 */
object UserTable: LongIdTable("users") {
    val username = varchar("username", 255).uniqueIndex()

    val firstName = varchar("first_name", 255)
    val lastName = varchar("last_name", 255)

    val address = varchar("address", 255).nullable()
    val zipcode = varchar("zipcode", 24).nullable()
    val birthDate = date("birth_date").nullable()

    val avatar = blob("avatar").nullable()

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()
}

/**
 * 코루틴 환경에서 사용자 데이터를 표현하는 엔티티입니다.
 */
class UserEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<UserEntity>(UserTable)

    var username by UserTable.username

    var firstName by UserTable.firstName
    var lastName by UserTable.lastName

    var address by UserTable.address
    var zipcode by UserTable.zipcode
    var birthDate by UserTable.birthDate

    var avatar by UserTable.avatar

    var createdAt by UserTable.createdAt
    var updatedAt by UserTable.updatedAt

    override fun equals(other: Any?): Boolean = idEquals(other)
    override fun hashCode(): Int = idHashCode()
    override fun toString(): String = entityToStringBuilder()
        .add("firstName", firstName)
        .add("lastName", lastName)
        .add("address", address)
        .add("zipcode", zipcode)
        .add("birthDate", birthDate)
        .add("createdAt", createdAt)
        .add("updatedAt", updatedAt)
        .toString()
}

/**
 * 사용자 도메인의 캐시/DB 전달용 레코드입니다.
 */
data class UserRecord(
    val id: Long = 0L,
    val username: String,
    val firstName: String,
    val lastName: String,
    val address: String? = null,
    val zipcode: String? = null,
    val birthDate: LocalDate? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
): Serializable {
    var avatar: ByteArray? = null
    fun withId(id: Long) = copy(id = id)
}

/**
 * 조회 결과를 [UserRecord]로 변환합니다.
 */
fun ResultRow.toUserRecord() = UserRecord(
    id = this[UserTable.id].value,
    username = this[UserTable.username],
    firstName = this[UserTable.firstName],
    lastName = this[UserTable.lastName],
    address = this[UserTable.address],
    zipcode = this[UserTable.zipcode],
    birthDate = this[UserTable.birthDate],
    createdAt = this[UserTable.createdAt],
    updatedAt = this[UserTable.updatedAt]
).also {
    it.avatar = this[UserTable.avatar]?.bytes
}

/**
 * [UserEntity]를 [UserRecord]로 변환합니다.
 */
fun UserEntity.toUserRecord() = UserRecord(
    id = this.id.value,
    username = this.username,
    firstName = this.firstName,
    lastName = this.lastName,
    address = this.address,
    zipcode = this.zipcode,
    birthDate = this.birthDate,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
).also {
    it.avatar = this.avatar?.bytes
}

/**
 * 테스트용 임의 사용자 레코드를 생성합니다.
 */
fun newUserRecord(newId: Long = 0L) = UserRecord(
    id = newId,
    username = faker.credentials().username() + "." + Base58.randomString(4),
    firstName = faker.name().firstName(),
    lastName = faker.name().lastName(),
    address = faker.address().fullAddress(),
    zipcode = faker.address().zipCode(),
    birthDate = LocalDate.now(),
    createdAt = null,
    updatedAt = null
).also {
    it.avatar = faker.image().base64JPG().toByteArray()
}

/**
 * 테스트용 임의 [UserEntity]를 생성합니다.
 */
fun newUserEntity(faker: Faker): UserEntity = UserEntity.new {
    username = faker.credentials().username() + "." + Base58.randomString(4)
    firstName = faker.name().firstName()
    lastName = faker.name().lastName()
    address = faker.address().fullAddress()
    zipcode = faker.address().zipCode()
    birthDate = LocalDate.now()
    avatar = faker.image().base64JPG().toByteArray().toExposedBlob()
}.apply {
    flush()
}
