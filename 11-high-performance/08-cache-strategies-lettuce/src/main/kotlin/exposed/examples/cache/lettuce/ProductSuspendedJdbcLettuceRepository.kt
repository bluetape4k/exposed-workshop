package exposed.examples.cache.lettuce

import io.bluetape4k.exposed.lettuce.repository.AbstractSuspendedJdbcLettuceRepository
import io.bluetape4k.exposed.lettuce.repository.ExposedLettuceCodecs
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.RedisCodec
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement

/**
 * [ProductJdbcLettuceRepository]와 같은 cache/key 계약을 suspend API로 제공합니다.
 * DB 직접 조회는 provider의 `suspendedTransactionAsync(Dispatchers.IO)` 경계를
 * 따르며, [kotlinx.coroutines.CancellationException]은 fallback으로 삼지 않습니다.
 */
class ProductSuspendedJdbcLettuceRepository(
    client: RedisClient,
    config: LettuceCacheConfig = ProductLettuceCacheConfig.default(),
    valueCodec: RedisCodec<String, ProductRecord> =
        ExposedLettuceCodecs.jackson3(ProductRecord::class.java),
): AbstractSuspendedJdbcLettuceRepository<Long, ProductRecord>(
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

    /** sync repository와 동일한 Redis key를 사용합니다. */
    override fun serializeKey(id: Long): String = "product-$id"

    override suspend fun invalidateByPattern(patterns: String, count: Int): Long {
        require(patterns == ProductLettuceCacheConfig.PRODUCT_PATTERN) {
            "상품 캐시 pattern은 ${ProductLettuceCacheConfig.PRODUCT_PATTERN}만 허용합니다."
        }
        return super.invalidateByPattern(patterns, count)
    }
}
