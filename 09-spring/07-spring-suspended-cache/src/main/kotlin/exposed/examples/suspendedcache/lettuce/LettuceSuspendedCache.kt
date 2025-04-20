package exposed.examples.suspendedcache.lettuce

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.flow.chunked

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceSuspendedCache<K: Any, V: Any>(
    val name: String,
    val commands: RedisCoroutinesCommands<String, V>,
    private val ttlSeconds: Long? = null,
) {

    private fun keyStr(key: K): String = "$name:$key"

    suspend fun get(key: K): V? = commands.get(keyStr(key))

    suspend fun put(key: K, value: V) {
        if (ttlSeconds != null) {
            commands.setex(keyStr(key), ttlSeconds, value)
        } else {
            commands.set(keyStr(key), value)
        }
    }

    suspend fun evict(key: K) {
        commands.del(keyStr(key))
    }

    suspend fun clear() {
        commands.keys("$name:*")
            .chunked(100)
            .collect { keys ->
                commands.del(*keys.toTypedArray())
            }
    }
}
