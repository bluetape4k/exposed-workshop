package exposed.examples.benchmark.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

/**
 * read-through 캐시의 hit/miss 비용과 DB 직접 조회 비용을 비교하는 벤치마크입니다.
 *
 * 실제 Redis/DB I/O 대신 Caffeine near-cache + 인메모리 저장소를 사용해
 * 캐시 계층 자체의 오버헤드 차이를 안정적으로 비교합니다.
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class ReadThroughCacheBenchmark {

    @Param("256", "4096")
    var payloadBytes: Int = 0

    private lateinit var db: Map<Long, UserPayload>
    private lateinit var cache: Cache<Long, UserPayload>
    private lateinit var missKeys: LongArray
    private val hotKey: Long = 1L
    private var missIndex: Int = 0

    @Setup(Level.Trial)
    fun setupTrial() {
        db = (1L..2048L).associateWith { id ->
            UserPayload(
                id = id,
                bytes = ByteArray(payloadBytes) { offset -> ((id + offset) % 127).toByte() }
            )
        }
        cache = Caffeine.newBuilder()
            .maximumSize(4096)
            .build()
        missKeys = LongArray(256) { index -> (index + 2).toLong() }
    }

    @Setup(Level.Iteration)
    fun setupIteration() {
        missIndex = 0
        cache.invalidateAll()
        cache.put(hotKey, db.getValue(hotKey))
    }

    @Benchmark
    fun dbOnlyRead(): UserPayload = db.getValue(hotKey)

    @Benchmark
    fun readThroughCacheHit(): UserPayload =
        cache.get(hotKey) { key -> db.getValue(key) }

    @Benchmark
    fun readThroughCacheMiss(): UserPayload {
        val key = missKeys[missIndex++ and (missKeys.size - 1)]
        cache.invalidate(key)
        return cache.get(key) { missKey -> db.getValue(missKey) }
    }
}

/**
 * payload 크기 변화에 따른 캐시 계층의 오버헤드를 비교하기 위한 샘플 데이터입니다.
 */
data class UserPayload(
    val id: Long,
    val bytes: ByteArray,
)
