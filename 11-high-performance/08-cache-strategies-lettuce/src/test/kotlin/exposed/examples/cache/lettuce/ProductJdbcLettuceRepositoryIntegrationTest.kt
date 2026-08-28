package exposed.examples.cache.lettuce

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/** Redis opt-in에서 sync read/write-through와 key namespace 계약을 검증합니다. */
@Tag("redis")
@Timeout(value = 45, unit = TimeUnit.SECONDS)
class ProductJdbcLettuceRepositoryIntegrationTest: AbstractExposedTest() {

    @Test
    fun `sync repository preserves database and cache contracts`() {
        RedisLettuceCacheFixture.awaitReady()
        val prefix = RedisLettuceCacheFixture.newPrefix()
        val repository = ProductJdbcLettuceRepository(
            client = RedisLettuceCacheFixture.client,
            config = RedisLettuceCacheFixture.config(prefix)
        )

        try {
            withTables(TestDB.H2, ProductTable) {
                val first = seedProduct("lettuce-1", "첫 상품", 1000)
                val second = seedProduct("lettuce-2", "둘째 상품", 2000)
                commit()

                assertEquals(first, repository.get(first.id))
                assertEquals(first, repository.get(first.id))
                assertEquals(
                    mapOf(first.id to first, second.id to second),
                    repository.getAll(listOf(first.id, second.id, Long.MAX_VALUE))
                )

                val listed =
                    repository.findAll(
                        limit = 2,
                        sortBy = ProductTable.id,
                        sortOrder = SortOrder.ASC
                    ) { ProductTable.id greater 0L }
                assertEquals(listOf(first.id, second.id), listed.map { it.id })

                assertCacheWarm(prefix, first)
                assertInvalidationAndWrite(repository, prefix, first, second)
                assertNamespaceIsolationAndClear(repository, prefix, first)
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

    private fun assertCacheWarm(prefix: String, first: ProductRecord) {
        val firstKey = RedisLettuceCacheFixture.key(prefix, first.id)
        assertTrue(RedisLettuceCacheFixture.readString(firstKey).orEmpty().isNotBlank())
    }

    private fun assertInvalidationAndWrite(
        repository: ProductJdbcLettuceRepository,
        prefix: String,
        first: ProductRecord,
        second: ProductRecord,
    ) {
        repository.invalidate(first.id)
        assertNull(RedisLettuceCacheFixture.readString(RedisLettuceCacheFixture.key(prefix, first.id)))
        assertEquals(first, repository.findByIdFromDb(first.id))

        repository.put(first.id, first.copy(name = "갱신 상품", priceCents = 1100))
        repository.putAll(
            mapOf(
                first.id to first.copy(name = "배치 첫 상품"),
                second.id to second.copy(name = "배치 둘째 상품")
            ),
            batchSize = 1
        )
        assertEquals("배치 첫 상품", repository.findByIdFromDb(first.id)?.name)
        assertEquals("배치 둘째 상품", repository.findByIdFromDb(second.id)?.name)

        repository.invalidateAll(listOf(first.id, second.id))
        assertNull(RedisLettuceCacheFixture.readString(RedisLettuceCacheFixture.key(prefix, first.id)))
        assertEquals(2L, repository.countFromDb())
    }

    private fun assertNamespaceIsolationAndClear(
        repository: ProductJdbcLettuceRepository,
        prefix: String,
        first: ProductRecord,
    ) {
        val isolatedPrefix = RedisLettuceCacheFixture.newPrefix()
        try {
            RedisLettuceCacheFixture.writeString(
                RedisLettuceCacheFixture.key(isolatedPrefix, 999),
                "isolated"
            )
            repository.put(first.id, first)
            assertTrue(repository.invalidateByPattern("product-*", count = 100) >= 1)
            assertNull(RedisLettuceCacheFixture.readString(RedisLettuceCacheFixture.key(prefix, first.id)))
            assertEquals(
                "isolated",
                RedisLettuceCacheFixture.readString(RedisLettuceCacheFixture.key(isolatedPrefix, 999))
            )
            assertThrows(IllegalArgumentException::class.java) {
                repository.invalidateByPattern("*", count = 100)
            }
            repository.clear()
            assertNull(RedisLettuceCacheFixture.readString(RedisLettuceCacheFixture.key(prefix, first.id)))
        } finally {
            RedisLettuceCacheFixture.cleanup(isolatedPrefix)
        }
    }
}
