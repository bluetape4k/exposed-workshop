package exposed.examples.cache.lettuce

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.lettuce.map.ExposedLettuceLoadedMap
import io.bluetape4k.exposed.lettuce.repository.ExposedLettuceCodecs
import io.bluetape4k.redis.lettuce.map.MapWriter
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.TimeUnit

/** Redis transport/decode 장애와 trusted-cache 경계를 검증합니다. */
@Tag("redis")
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class RedisLettuceCacheFailureIntegrationTest: AbstractExposedTest() {

    @Test
    fun `malformed payload falls back to database and altered payload is trusted`() {
        RedisLettuceCacheFixture.awaitReady()
        val prefix = RedisLettuceCacheFixture.newPrefix()
        val repository = ProductJdbcLettuceRepository(
            RedisLettuceCacheFixture.client,
            RedisLettuceCacheFixture.config(prefix)
        )

        try {
            withTables(TestDB.H2, ProductTable) {
                val product = seedProduct("failure-1", "정상 상품", 5000)
                commit()
                val key = RedisLettuceCacheFixture.key(prefix, product.id)

                RedisLettuceCacheFixture.writeString(key, "{malformed-json")
                assertEquals(product, repository.get(product.id))

                val altered = product.copy(name = "변조된 캐시 값")
                RedisLettuceCacheFixture.writeValue(
                    key,
                    altered,
                    ExposedLettuceCodecs.jackson3(ProductRecord::class.java)
                )
                assertEquals(altered, repository.get(product.id))
            }
        } finally {
            repository.close()
            RedisLettuceCacheFixture.cleanup(prefix)
        }
    }

    @Test
    fun `transport interruption falls back and same client reconnects`() {
        RedisLettuceCacheFixture.awaitReady()
        val prefix = RedisLettuceCacheFixture.newPrefix()
        val repository = ProductJdbcLettuceRepository(
            RedisLettuceCacheFixture.client,
            RedisLettuceCacheFixture.config(prefix)
        )

        try {
            withTables(TestDB.H2, ProductTable) {
                val product = seedProduct("failure-2", "재연결 상품", 6000)
                commit()
                assertEquals(product, repository.get(product.id))

                // Testcontainers maps a new host port after stop/start, so stopping the
                // container would invalidate the intentionally shared RedisClient URI.
                // Kill only existing Redis client transports and keep the server endpoint
                // stable; Lettuce must then fall back and reconnect through the same client.
                RedisLettuceCacheFixture.redis.execInContainer(
                    "redis-cli",
                    "CLIENT",
                    "KILL",
                    "TYPE",
                    "normal",
                    "SKIPME",
                    "yes"
                )
                assertEquals(product, repository.get(product.id))

                RedisLettuceCacheFixture.awaitReady(Duration.ofSeconds(20))
                assertEquals(product, repository.get(product.id))
            }
        } finally {
            assertDoesNotThrow { repository.close() }
            RedisLettuceCacheFixture.cleanup(prefix)
        }
    }

    @Test
    fun `second batch writer failure preserves first chunk state`() {
        RedisLettuceCacheFixture.awaitReady()
        val prefix = RedisLettuceCacheFixture.newPrefix()
        val first = ProductRecord(1L, "partial-1", "첫 부분", 7000)
        val second = ProductRecord(2L, "partial-2", "둘째 부분", 8000)
        val writtenChunks = mutableListOf<Map<Long, ProductRecord>>()
        val writer =
            object : MapWriter<Long, ProductRecord> {
                override fun write(map: Map<Long, ProductRecord>) {
                    if (writtenChunks.isNotEmpty()) {
                        throw IllegalStateException("second batch chunk rejected")
                    }
                    writtenChunks += map
                }

                override fun delete(keys: Collection<Long>) = Unit
            }
        val map =
            ExposedLettuceLoadedMap(
                client = RedisLettuceCacheFixture.client,
                writer = writer,
                config = RedisLettuceCacheFixture.config(prefix),
                keySerializer = { id -> "product-$id" },
                valueCodec = ExposedLettuceCodecs.jackson3(ProductRecord::class.java)
            )

        try {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    map.putAll(linkedMapOf(first.id to first, second.id to second), batchSize = 1)
                }

            assertEquals("second batch chunk rejected", error.message)
            assertEquals(listOf(mapOf(first.id to first)), writtenChunks)
            assertNotNull(
                RedisLettuceCacheFixture.readString(
                    RedisLettuceCacheFixture.key(prefix, first.id)
                )
            )
            assertNull(
                RedisLettuceCacheFixture.readString(
                    RedisLettuceCacheFixture.key(prefix, second.id)
                )
            )
        } finally {
            map.close()
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
