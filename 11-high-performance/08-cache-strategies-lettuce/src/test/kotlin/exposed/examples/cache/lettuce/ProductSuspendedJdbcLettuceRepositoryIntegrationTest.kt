package exposed.examples.cache.lettuce

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTablesSuspending
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/** Redis opt-in에서 suspend repository가 sync와 동일한 JDBC/cache 계약을 지키는지 검증합니다. */
@Tag("redis")
@Timeout(value = 45, unit = TimeUnit.SECONDS)
class ProductSuspendedJdbcLettuceRepositoryIntegrationTest: AbstractExposedTest() {

    @Test
    fun `suspended repository preserves sync parity`() = runBlocking {
        RedisLettuceCacheFixture.awaitReady()
        val prefix = RedisLettuceCacheFixture.newPrefix()
        val repository = ProductSuspendedJdbcLettuceRepository(
            client = RedisLettuceCacheFixture.client,
            config = RedisLettuceCacheFixture.config(prefix)
        )

        try {
            withTablesSuspending(TestDB.H2, ProductTable) {
                val first = seedProduct("suspend-1", "서스펜드 상품", 3000)
                val second = seedProduct("suspend-2", "서스펜드 둘째", 4000)
                commit()

                assertEquals(first, repository.get(first.id))
                assertEquals(
                    mapOf(first.id to first, second.id to second),
                    repository.getAll(listOf(first.id, second.id, Long.MAX_VALUE))
                )
                assertEquals(
                    listOf(first.id, second.id),
                    repository.findAll(
                        limit = 2,
                        sortBy = ProductTable.id,
                        sortOrder = SortOrder.ASC
                    ) { ProductTable.id greater 0L }.map { it.id }
                )

                repository.put(first.id, first.copy(name = "서스펜드 갱신"))
                repository.putAll(
                    mapOf(second.id to second.copy(name = "서스펜드 배치")),
                    batchSize = 1
                )
                assertEquals("서스펜드 갱신", repository.findByIdFromDb(first.id)?.name)
                assertEquals("서스펜드 배치", repository.findByIdFromDb(second.id)?.name)

                repository.invalidate(first.id)
                assertNull(RedisLettuceCacheFixture.readString(RedisLettuceCacheFixture.key(prefix, first.id)))
                repository.invalidateAll(listOf(second.id))
                assertNull(RedisLettuceCacheFixture.readString(RedisLettuceCacheFixture.key(prefix, second.id)))
                val invalidationFailure =
                    try {
                        repository.invalidateByPattern("untrusted-*", count = 100)
                        null
                    } catch (e: IllegalArgumentException) {
                        e
                    }
                assertTrue(invalidationFailure != null)
                repository.clear()
            }
        } finally {
            repository.close()
            RedisLettuceCacheFixture.cleanup(prefix)
        }
    }

    private fun JdbcTransaction.seedProduct(
        sku: String,
        name: String,
        priceCents: Long,
    ): ProductRecord {
        val id =
            ProductTable.insertAndGetId {
                it[ProductTable.sku] = sku
                it[ProductTable.name] = name
                it[ProductTable.priceCents] = priceCents
            }.value
        return ProductRecord(id, sku, name, priceCents)
    }
}
