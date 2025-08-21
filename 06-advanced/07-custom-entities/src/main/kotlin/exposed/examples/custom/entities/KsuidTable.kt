package exposed.examples.custom.entities

import io.bluetape4k.idgenerators.ksuid.Ksuid
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

/**
 * Primary Key 를 Client에서 생성되는 [Ksuid] 값으로 사용하는 Table
 *
 * ### 사용 예제
 *
 * ```kotlin
 * object Products: KsuidTable("products") {
 *     val name = varchar("name", 100)
 *     val price = integer("price")
 * }
 *
 * class ProductEntity(id: KsuidEntityID): KsuidEntity(id) {
 *     var name by Products.name
 *     var price by Products.price
 *     companion object : KsuidEntityClass<ProductEntity>(Products)
 * }
 *
 * // 코루틴 기반 트랜잭션 예시
 * suspend fun createProduct() = newSuspendedTransaction {
 *     val product = ProductEntity.new {
 *         name = "노트북"
 *         price = 1500000
 *     }
 *     println(product.id.value) // 생성된 Ksuid 값
 * }
 *
 * // batchInsert 예시
 * suspend fun batchInsertProducts() = newSuspendedTransaction {
 *     Products.batchInsert(listOf("A", "B", "C")) { name ->
 *         this[Products.name] = name
 *         this[Products.price] = 1000
 *     }
 * }
 * ```
 *
 * @param name 테이블명 (기본값: "")
 * @param columnName PK 컬럼명 (기본값: "id")
 * @author debop
 */
open class KsuidTable(name: String = "", columnName: String = "id"): IdTable<String>(name) {
    /**
     * PK 컬럼. Client에서 생성한 Ksuid 값을 사용하며, 27자 문자열로 저장된다.
     * 값이 지정되지 않으면 [Ksuid.nextIdAsString]을 통해 자동 생성된다.
     */
    final override val id: Column<EntityID<String>> =
        varchar(columnName, 27).clientDefault { Ksuid.nextIdAsString() }.entityId()

    /**
     * PK 설정 (id 컬럼)
     */
    final override val primaryKey = PrimaryKey(id)
}

/**
 * Ksuid 기반 Entity의 ID 타입 별칭
 */
typealias KsuidEntityID = EntityID<String>

/**
 * Entity ID 를 [Ksuid]로 생성한 문자열을 사용하는 Entity
 *
 * @param id 엔티티의 PK (KsuidEntityID)
 */
open class KsuidEntity(id: KsuidEntityID): Entity<String>(id)

/**
 * [KsuidEntity]를 생성하는 EntityClass
 *
 * @param E 엔티티 타입
 * @param table 매핑되는 테이블
 * @param entityType 엔티티 클래스 타입 (선택)
 * @param entityCtor 엔티티 생성자 (선택)
 */
open class KsuidEntityClass<out E: KsuidEntity>(
    table: KsuidTable,
    entityType: Class<E>? = null,
    entityCtor: ((KsuidEntityID) -> E)? = null,
): EntityClass<String, E>(table, entityType, entityCtor)
