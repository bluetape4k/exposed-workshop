package exposed.examples.benchmark.cache

import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.Database
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
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import java.util.Random
import java.util.concurrent.TimeUnit

/**
 * 워크로드 패턴 열거형: 읽기/쓰기 비율을 정의합니다.
 *
 * - READ_HEAVY: 90% 읽기 / 10% 쓰기
 * - WRITE_HEAVY: 10% 읽기 / 90% 쓰기
 */
enum class WorkloadPattern(val readRatio: Double) {
    READ_HEAVY(0.9),
    WRITE_HEAVY(0.1),
}

/**
 * 캐시 전략 열거형: 벤치마크에서 @Param으로 사용됩니다.
 */
enum class StrategyType {
    NO_CACHE,
    READ_THROUGH,
    WRITE_THROUGH,
}

/**
 * 실제 PostgreSQL DB를 대상으로 NoCache, ReadThrough, WriteThrough 캐시 전략을
 * 읽기 중심(90/10) 및 쓰기 중심(10/90) 워크로드에서 비교하는 벤치마크입니다.
 *
 * 가설:
 * - 읽기 중심 워크로드에서 ReadThrough는 NoCache 대비 3배 이상 빠를 것
 * - 쓰기 중심 워크로드에서 WriteThrough는 NoCache 대비 2배 미만의 오버헤드를 보일 것
 * - 쓰기 중심 워크로드에서 ReadThrough는 캐시 무효화로 인해 NoCache보다 느릴 것
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class CacheStrategyComparisonBenchmark {

    companion object: KLogging() {
        private const val SEED_ROW_COUNT = 1000
        private const val MAX_KEY = 1000L
    }

    @Param("NO_CACHE", "READ_THROUGH", "WRITE_THROUGH")
    lateinit var strategyType: String

    @Param("READ_HEAVY", "WRITE_HEAVY")
    lateinit var workloadPattern: String

    @Param("256", "4096")
    var payloadBytes: Int = 0

    private lateinit var dataSource: HikariDataSource
    private lateinit var db: Database
    private lateinit var strategy: CacheStrategy
    private lateinit var random: Random
    private lateinit var pattern: WorkloadPattern
    private var operationCounter: Long = 0

    @Setup(Level.Trial)
    fun setupTrial() {
        dataSource = CacheBenchmarkSetup.createDataSource()
        db = CacheBenchmarkSetup.setupDatabase(dataSource)
        CacheBenchmarkSetup.createTables(db)
        CacheBenchmarkSetup.seedData(db, SEED_ROW_COUNT, payloadBytes)

        strategy = when (StrategyType.valueOf(strategyType)) {
            StrategyType.NO_CACHE -> NoCacheStrategy(db)
            StrategyType.READ_THROUGH -> ReadThroughStrategy(db)
            StrategyType.WRITE_THROUGH -> WriteThroughStrategy(db)
        }

        pattern = WorkloadPattern.valueOf(workloadPattern)
        random = Random(42)
        operationCounter = 0
    }

    @Setup(Level.Iteration)
    fun setupIteration() {
        random = Random(42)
        operationCounter = 0
    }

    @TearDown(Level.Trial)
    fun tearDownTrial() {
        runCatching { dataSource.close() }
    }

    /**
     * 워크로드 비율에 따라 읽기 또는 쓰기 작업을 수행합니다.
     */
    @Benchmark
    fun mixedWorkload(): Any? {
        operationCounter++
        val key = (random.nextLong(MAX_KEY) + 1).coerceIn(1, MAX_KEY)
        val isRead = random.nextDouble() < pattern.readRatio

        return if (isRead) {
            strategy.read(key)
        } else {
            val newPayload = CachePayload(
                id = key,
                payload = CacheBenchmarkSetup.generatePayload(
                    id = operationCounter,
                    size = payloadBytes,
                ),
            )
            strategy.write(key, newPayload)
            newPayload
        }
    }
}
