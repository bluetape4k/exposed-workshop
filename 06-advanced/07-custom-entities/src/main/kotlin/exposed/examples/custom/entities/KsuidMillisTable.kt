package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.ksuid.KsuidMillis
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

/**
 * Primary Key 를 Client에서 생성되는 [KsuidMillis] 값으로 사용하는 Table
 *
 * ### 사용 예제
 *
 * ```kotlin
 * object Users: KsuidMillisTable("users") {
 *     val name = varchar("name", 50)
 * }
 *
 * class UserEntity(id: KsuidMillisEntityID): KsuidMillisEntity(id) {
 *     var name by Users.name
 *     companion object : KsuidMillisEntityClass<UserEntity>(Users)
 * }
 *
 * // 엔티티 생성 예시
 * transaction {
 *     val user = UserEntity.new {
 *         name = "홍길동"
 *     }
 *     println(user.id.value) // 생성된 KsuidMillis 값
 * }
 * ```
 *
 * @param name 테이블명 (기본값: "")
 * @param columnName PK 컬럼명 (기본값: "id")
 * @author debop
 */
open class KsuidMillisTable(name: String = "", columnName: String = "id"): IdTable<String>(name) {
    /**
     * PK 컬럼. Client에서 생성한 KsuidMillis 값을 사용하며, 27자 문자열로 저장된다.
     * 값이 지정되지 않으면 [KsuidMillis.nextIdAsString]을 통해 자동 생성된다.
     */
    final override val id: Column<EntityID<String>> =
        varchar(columnName, 27).clientDefault { KsuidMillis.nextIdAsString() }.entityId()

    /**
     * PK 설정 (id 컬럼)
     */
    final override val primaryKey = PrimaryKey(id)
}

/**
 * KsuidMillis 기반 Entity의 ID 타입 별칭
 */
typealias KsuidMillisEntityID = EntityID<String>

/**
 * Entity ID 를 [KsuidMillis]로 생성한 문자열을 사용하는 Entity
 *
 * @param id 엔티티의 PK (KsuidMillisEntityID)
 */
open class KsuidMillisEntity(id: KsuidMillisEntityID): Entity<String>(id)

/**
 * [KsuidMillisEntity]를 생성하는 EntityClass
 *
 * @param E 엔티티 타입
 * @param table 매핑되는 테이블
 * @param entityType 엔티티 클래스 타입 (선택)
 * @param entityCtor 엔티티 생성자 (선택)
 */
open class KsuidMillisEntityClass<out E: KsuidMillisEntity>(
    table: KsuidMillisTable,
    entityType: Class<E>? = null,
    entityCtor: ((KsuidMillisEntityID) -> E)? = null,
): EntityClass<String, E>(table, entityType, entityCtor)
