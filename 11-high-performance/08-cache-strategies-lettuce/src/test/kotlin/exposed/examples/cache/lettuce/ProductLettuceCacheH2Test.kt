package exposed.examples.cache.lettuce

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.lettuce.map.ExposedLettuceSuspendedLoadedMap
import io.bluetape4k.exposed.lettuce.repository.ExposedLettuceCodecs
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.RedisCodec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

/** Redis 없이도 검증할 수 있는 JDBC 매핑·설정 계약입니다. */
class ProductLettuceCacheH2Test: AbstractExposedTest() {

    @Test
    fun `default config selects remote-only product namespace`() {
        val config = ProductLettuceCacheConfig.default()

        assertEquals("workshop:products", config.keyPrefix)
        assertFalse(config.nearCacheEnabled)
        assertNotNull(ExposedLettuceCodecs.jackson3(ProductRecord::class.java))
    }

    @Test
    fun `database mapping works without touching Redis`() {
        val client = mockk<RedisClient>(relaxed = true)
        val repository = ProductJdbcLettuceRepository(client)

        withTables(TestDB.H2, ProductTable) {
            val id =
                ProductTable.insertAndGetId {
                    it[sku] = "h2-sku"
                    it[name] = "H2 상품"
                    it[priceCents] = 1299
                }.value
            commit()

            assertEquals(ProductRecord(id, "h2-sku", "H2 상품", 1299), repository.findByIdFromDb(id))
            assertEquals(1L, repository.countFromDb())
            assertEquals(1, repository.findAllFromDb(listOf(id)).size)
        }

        verify(exactly = 0) { client.connect(any<io.lettuce.core.codec.RedisCodec<String, ProductRecord>>()) }
    }

    @Test
    fun `putAll validates batch size before cache access`() {
        val repository = ProductJdbcLettuceRepository(mockk())

        assertThrows(IllegalArgumentException::class.java) {
            repository.putAll(emptyMap(), batchSize = 0)
        }
    }

    @Test
    fun `suspended redis await propagates cancellation`() = runBlocking {
        val client = mockk<RedisClient>()
        val connection = mockk<StatefulRedisConnection<String, ProductRecord>>(relaxed = true)
        val commands = mockk<RedisAsyncCommands<String, ProductRecord>>()
        val pending = mockk<RedisFuture<ProductRecord>>(relaxed = true)
        val pendingFuture = CompletableFuture<ProductRecord>()
        val codec = ExposedLettuceCodecs.jackson3(ProductRecord::class.java)
        every { client.connect(any<RedisCodec<String, ProductRecord>>()) } returns connection
        every { connection.async() } returns commands
        every { commands.get(any()) } returns pending
        every { pending.toCompletableFuture() } returns pendingFuture

        val repository =
            ExposedLettuceSuspendedLoadedMap<Long, ProductRecord>(
                client = client,
                config = ProductLettuceCacheConfig.default(),
                valueCodec = codec
            )
        try {
            val job = launch(Dispatchers.Default) { repository.get(1L) }
            verify(timeout = 1_000) { commands.get(any()) }

            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertTrue(pendingFuture.isCancelled)
        } finally {
            repository.close()
        }
    }
}
