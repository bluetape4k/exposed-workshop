package exposed.examples.cache.lettuce

import io.bluetape4k.exposed.lettuce.repository.AbstractJdbcLettuceRepository
import io.bluetape4k.exposed.lettuce.repository.ExposedLettuceCodecs
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.RedisCodec
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement

/**
 * Exposed JDBC와 Lettuce remote cache를 직접 연결한 상품 repository입니다.
 *
 * provider lifecycle은 호출자가 소유한 [RedisClient]에 남기고, repository는
 * 자신이 만든 Redis connection만 닫습니다. 기본 codec은 반드시 명시적인
 * [ExposedLettuceCodecs.jackson3] Jackson 3 codec입니다.
 */
class ProductJdbcLettuceRepository(
    client: RedisClient,
    config: LettuceCacheConfig = ProductLettuceCacheConfig.default(),
    valueCodec: RedisCodec<String, ProductRecord> =
        ExposedLettuceCodecs.jackson3(ProductRecord::class.java),
): AbstractJdbcLettuceRepository<Long, ProductRecord>(
    client = client,
    config = config,
    valueCodec = valueCodec
) {
    init {
        ProductLettuceCacheConfig.requireAllowed(config)
    }

    override val table: IdTable<Long> = ProductTable

    override fun ResultRow.toEntity(): ProductRecord =
        ProductRecord(
            id = this[ProductTable.id].value,
            sku = this[ProductTable.sku],
            name = this[ProductTable.name],
            priceCents = this[ProductTable.priceCents]
        )

    override fun extractId(entity: ProductRecord): Long = entity.id

    override fun UpdateStatement.updateEntity(entity: ProductRecord) {
        this[ProductTable.sku] = entity.sku
        this[ProductTable.name] = entity.name
        this[ProductTable.priceCents] = entity.priceCents
    }

    override fun BatchInsertStatement.insertEntity(entity: ProductRecord) {
        this[ProductTable.id] = entity.id
        this[ProductTable.sku] = entity.sku
        this[ProductTable.name] = entity.name
        this[ProductTable.priceCents] = entity.priceCents
    }

    /** Redis key의 상품 ID 부분을 고정해 pattern allow-list와 일치시킵니다. */
    override fun serializeKey(id: Long): String = "product-$id"

    override fun invalidateByPattern(patterns: String, count: Int): Long {
        require(patterns == ProductLettuceCacheConfig.PRODUCT_PATTERN) {
            "상품 캐시 pattern은 ${ProductLettuceCacheConfig.PRODUCT_PATTERN}만 허용합니다."
        }
        return super.invalidateByPattern(patterns, count)
    }
}
