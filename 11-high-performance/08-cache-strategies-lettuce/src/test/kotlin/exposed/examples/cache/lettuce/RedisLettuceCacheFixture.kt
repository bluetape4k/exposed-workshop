package exposed.examples.cache.lettuce

import io.bluetape4k.codec.Base58
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.RedisURI
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import java.time.Duration

/**
 * Redis 통합 테스트가 공유하는 서버·클라이언트·namespace 수명주기 보조 객체입니다.
 *
 * 클라이언트는 이 객체에서 한 번만 [ShutdownQueue]에 등록합니다. 테스트는
 * repository와 connection만 닫고 RedisClient를 직접 종료하지 않습니다.
 */
object RedisLettuceCacheFixture {
    private const val NAMESPACE = "workshop:products"
    private val readinessTimeout = Duration.ofSeconds(15)

    val redis: RedisServer by lazy { RedisServer.Launcher.redis }

    val client by lazy {
        val uri =
            RedisURI.builder()
                .withHost(redis.host)
                .withPort(redis.port)
                .withTimeout(Duration.ofSeconds(2))
                .build()
        LettuceClients.clientOf(uri).also { redisClient ->
            ShutdownQueue.register { redisClient.shutdown() }
        }
    }

    fun newPrefix(): String = "$NAMESPACE:${Base58.randomString(8)}"

    fun config(prefix: String): LettuceCacheConfig =
        ProductLettuceCacheConfig.default().copy(keyPrefix = prefix)

    fun key(prefix: String, id: Long): String = "$prefix:product-$id"

    fun awaitReady(timeout: Duration = readinessTimeout) {
        val deadline = System.nanoTime() + timeout.toNanos()
        var lastFailure: Throwable? = null
        while (System.nanoTime() < deadline) {
            runCatching {
                val connection = client.connect(StringCodec.UTF8)
                try {
                    connection.sync().ping()
                } finally {
                    connection.close()
                }
            }.onFailure { lastFailure = it }
                .onSuccess { return }
            Thread.sleep(50)
        }
        throw IllegalStateException("Redis readiness deadline exceeded", lastFailure)
    }

    fun readString(key: String): String? = withStringConnection { it.sync().get(key) }

    fun writeString(key: String, value: String) {
        withStringConnection { it.sync().set(key, value) }
    }

    fun <V> writeValue(key: String, value: V, codec: RedisCodec<String, V>) {
        val connection = client.connect(codec)
        try {
            connection.sync().set(key, value)
        } finally {
            connection.close()
        }
    }

    /** 지정한 literal namespace 아래의 키만 SCAN + UNLINK로 정리합니다. */
    fun cleanup(prefix: String) {
        runCatching {
            withStringConnection { connection ->
                val commands = connection.sync()
                var cursor: ScanCursor = ScanCursor.INITIAL
                do {
                    val result = commands.scan(cursor, ScanArgs.Builder.matches("$prefix:*").limit(100))
                    if (result.keys.isNotEmpty()) {
                        commands.unlink(*result.keys.toTypedArray())
                    }
                    cursor = result
                } while (!cursor.isFinished)
            }
        }
    }

    private fun <T> withStringConnection(block: (StatefulRedisConnection<String, String>) -> T): T {
        val connection = client.connect(StringCodec.UTF8)
        return try {
            block(connection)
        } finally {
            connection.close()
        }
    }
}
