package exposed.examples.cache.lettuce

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import java.io.Serializable

/** Lettuce JDBC 예제가 사용하는 상품 테이블입니다. */
object ProductTable: LongIdTable("lettuce_products") {
    val sku = varchar("sku", 64)
    val name = varchar("name", 200)
    val priceCents = long("price_cents")
}

/**
 * Redis에 저장하는 상품 값입니다.
 *
 * 값 코덱은 repository 생성 시 명시한 Jackson 3 codec이며, 캐시 값은
 * 인증·권한이나 업무 원장의 진실(source of truth)로 사용하지 않습니다.
 */
data class ProductRecord(
    val id: Long,
    val sku: String,
    val name: String,
    val priceCents: Long,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
