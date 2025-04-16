package exposed.examples.jpa.ex05_auditable

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityBatchUpdate
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

interface Auditable {
    val createdBy: String?
    val createdAt: Instant?
    val updatedBy: String?
    val updatedAt: Instant?
}

object UserContext {
    const val DEFAULT_USERNAME = "system"
    val CURRENT_USER: ScopedValue<String?> = ScopedValue.newInstance()

    fun <T> withUser(username: String, block: () -> T): T {
        return ScopedValue.where(CURRENT_USER, username).call(block)
    }

    fun getCurrentUser(): String =
        runCatching { CURRENT_USER.get() }.getOrNull() ?: DEFAULT_USERNAME
}

abstract class AuditableIdTable<ID: Any>(name: String = ""): IdTable<ID>(name) {

    val createdBy = varchar("created_by", 50).clientDefault { UserContext.getCurrentUser() }.nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp).nullable()

    val updatedBy = varchar("updated_by", 50).nullable()
    val updatedAt = timestamp("updatedAt_at").nullable()

}

abstract class AuditableEntity<ID: Any>(id: EntityID<ID>): Entity<ID>(id), Auditable {

    companion object: KLogging()

    override var createdBy: String? = null
    override var createdAt: Instant? = null
    override var updatedBy: String? = null
    override var updatedAt: Instant? = null

    // 엔티티가 업데이트될 때 실행되는 메서드 (flush 호출 전에 호출됨)
    override fun flush(batch: EntityBatchUpdate?): Boolean {
        // 엔티티에 변경된 필드가 있는 경우
        // isNewEntity() 가 internal 이라 사용할 수 없음
        if (writeValues.isNotEmpty() && createdAt != null) {
            log.debug { "entity is updated, setting updatedAt and updatedBy" }
            // 업데이트 시간을 현재로 설정
            updatedAt = Instant.now()
            updatedBy = UserContext.getCurrentUser()
        }
        if (createdAt == null) {
            // 생성 시간이 null 인 경우, 생성 시간과 생성자를 설정
            log.debug { "entity is created, setting createdAt and createdBy" }
            createdAt = Instant.now()
            createdBy = UserContext.getCurrentUser()
        }
        return super.flush(batch)
    }
}


abstract class AuditableIntIdTable(name: String = ""): AuditableIdTable<Int>(name) {
    final override val id = integer("id").autoIncrement().entityId()
    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

abstract class AuditableLongIdTable(name: String = ""): AuditableIdTable<Long>(name) {
    final override val id = long("id").autoIncrement().entityId()
    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

abstract class AuditableUUIDIdTable(name: String = ""): AuditableIdTable<UUID>(name) {
    final override val id = uuid("id").clientDefault { UUID.randomUUID() }.entityId()
    override val primaryKey: PrimaryKey = PrimaryKey(id)
}

abstract class AuditableIntEntity(id: EntityID<Int>): AuditableEntity<Int>(id)
abstract class AuditableLongEntity(id: EntityID<Long>): AuditableEntity<Long>(id)
abstract class AuditableUUIDEntity(id: EntityID<UUID>): AuditableEntity<UUID>(id)
