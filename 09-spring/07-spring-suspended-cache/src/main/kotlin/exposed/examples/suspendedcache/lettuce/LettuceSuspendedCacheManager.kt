package exposed.examples.suspendedcache.lettuce

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import java.util.concurrent.ConcurrentHashMap

class LettuceSuspendedCacheManager(
    val redisClient: RedisClient,
    val ttlSeconds: Long? = null,
    val codec: LettuceBinaryCodec<Any>? = null,
) {

    companion object: KLoggingChannel()

    private val caches = ConcurrentHashMap<String, LettuceSuspendedCache<out Any, out Any>>()

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    fun <K: Any, V: Any> getOrCreate(
        name: String,
        ttlSeconds: Long? = null,
        codec: LettuceBinaryCodec<V>? = null,
    ): LettuceSuspendedCache<K, V> {
        return caches.computeIfAbsent(name) {
            val conn = redisClient.connect(codec ?: this@LettuceSuspendedCacheManager.codec)
            val commands = conn.coroutines() as RedisCoroutinesCommands<String, V>

            LettuceSuspendedCache<K, V>(
                name = name,
                commands = commands,
                ttlSeconds = ttlSeconds ?: this@LettuceSuspendedCacheManager.ttlSeconds,
            )
        } as LettuceSuspendedCache<K, V>
    }
}
