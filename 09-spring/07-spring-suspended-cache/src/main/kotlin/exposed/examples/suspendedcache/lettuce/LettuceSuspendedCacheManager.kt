package exposed.examples.suspendedcache.lettuce

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import java.util.concurrent.ConcurrentHashMap

/**
 * Lettuce Redis 클라이언트를 사용하여 [LettuceSuspendedCache] 인스턴스를 관리하는 캐시 매니저.
 *
 * 이름별로 캐시 인스턴스를 생성하고 재사용합니다. 각 캐시는 별도의 Redis 연결을 가지며,
 * 코루틴 API를 통해 비동기 캐시 작업을 지원합니다.
 *
 * @property redisClient Lettuce Redis 클라이언트
 * @property ttlSeconds 기본 캐시 TTL(초). null이면 만료 없음
 * @property codec 기본 직렬화 코덱. null이면 기본 코덱 사용
 */
class LettuceSuspendedCacheManager(
    val redisClient: RedisClient,
    val ttlSeconds: Long? = null,
    val codec: LettuceBinaryCodec<Any>? = null,
) {

    companion object: KLoggingChannel()

    private val caches = ConcurrentHashMap<String, LettuceSuspendedCache<out Any, out Any>>()

    /**
     * 이름으로 캐시를 조회하거나 존재하지 않으면 새로 생성합니다.
     *
     * 동일한 이름으로 호출하면 기존에 생성된 캐시 인스턴스를 반환합니다.
     *
     * @param K 캐시 키의 타입
     * @param V 캐시 값의 타입
     * @param name 캐시 이름 (Redis 키 접두사로 사용)
     * @param ttlSeconds 이 캐시의 TTL(초). null이면 매니저의 기본 TTL 사용
     * @param codec 이 캐시의 직렬화 코덱. null이면 매니저의 기본 코덱 사용
     * @return 이름에 해당하는 [LettuceSuspendedCache] 인스턴스
     */
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
