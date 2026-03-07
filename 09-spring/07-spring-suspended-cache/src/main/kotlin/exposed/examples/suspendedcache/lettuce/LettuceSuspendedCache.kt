package exposed.examples.suspendedcache.lettuce

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.flow.chunked

/**
 * Lettuce Redis 코루틴 API를 사용하는 suspend 함수 기반 캐시 구현체.
 *
 * Redis를 백엔드 저장소로 사용하며, 코루틴 suspend 함수를 통해 비동기적으로
 * 캐시 데이터를 조회, 저장, 삭제할 수 있습니다.
 * 캐시 키는 `{name}:{key}` 형식으로 Redis에 저장됩니다.
 *
 * @param K 캐시 키의 타입
 * @param V 캐시 값의 타입
 * @property name 캐시 이름 (Redis 키 접두사로 사용)
 * @property commands Lettuce 코루틴 Redis 커맨드 인터페이스
 * @param ttlSeconds 캐시 항목의 TTL(초). null이면 만료 없이 저장
 */
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceSuspendedCache<K: Any, V: Any>(
    val name: String,
    val commands: RedisCoroutinesCommands<String, V>,
    private val ttlSeconds: Long? = null,
) {

    companion object: KLoggingChannel()

    /**
     * 캐시 키를 Redis 저장 키 문자열로 변환합니다.
     *
     * @param key 캐시 키
     * @return `{name}:{key}` 형식의 Redis 키 문자열
     */
    private fun keyStr(key: K): String = "$name:$key"

    /**
     * 캐시에서 키에 해당하는 값을 조회합니다.
     *
     * @param key 조회할 캐시 키
     * @return 캐시에 저장된 값, 존재하지 않으면 null
     */
    suspend fun get(key: K): V? = commands.get(keyStr(key))

    /**
     * 캐시에 키-값 쌍을 저장합니다.
     *
     * TTL이 설정된 경우 만료 시간과 함께 저장하고, 설정되지 않은 경우 만료 없이 저장합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     */
    suspend fun put(key: K, value: V) {
        if (ttlSeconds != null) {
            commands.setex(keyStr(key), ttlSeconds, value)
        } else {
            commands.set(keyStr(key), value)
        }
    }

    /**
     * 캐시에서 특정 키의 항목을 삭제합니다.
     *
     * @param key 삭제할 캐시 키
     */
    suspend fun evict(key: K) {
        commands.del(keyStr(key))
    }

    /**
     * 이 캐시의 모든 항목을 삭제합니다.
     *
     * `{name}:*` 패턴과 일치하는 모든 Redis 키를 100개씩 묶어 삭제합니다.
     */
    suspend fun clear() {
        commands.keys("$name:*")
            .chunked(100)
            .collect { keys ->
                commands.del(*keys.toTypedArray())
            }
    }
}
