package exposed.examples.suspendedcache.lettuce

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceSuspendedCache<K: Any, V: Any>(
    val name: String,
    val commands: RedisCoroutinesCommands<String, V>,
    private val ttlSeconds: Long? = null,
) {

    companion object: KLoggingChannel()

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
        // WARNING: KEYS 명령은 O(N) 전체 스캔으로 프로덕션에서 Redis를 차단할 수 있습니다.
        // SCAN을 사용하여 점진적으로 삭제합니다.
        var cursor: ScanCursor = ScanCursor.INITIAL
        do {
            val scanResult = commands.scan(cursor, ScanArgs().match("$name:*").limit(100))
                ?: break
            cursor = scanResult
            if (scanResult.keys.isNotEmpty()) {
                commands.del(*scanResult.keys.toTypedArray())
            }
        } while (!cursor.isFinished)
    }
}
