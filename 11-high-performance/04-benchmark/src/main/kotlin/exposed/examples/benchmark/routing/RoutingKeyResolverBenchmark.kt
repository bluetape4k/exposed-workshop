package exposed.examples.benchmark.routing

import exposed.examples.routing.datasource.ContextAwareRoutingKeyResolver
import io.bluetape4k.logging.KLogging
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
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
 * 11장 라우팅 예제의 핵심 경로인 라우팅 키 계산 비용을 측정합니다.
 *
 * 테넌트 헤더 유무와 read-only 트랜잭션 여부 조합에 따른
 * `tenant:mode` 문자열 생성 비용을 비교하는 용도입니다.
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class RoutingKeyResolverBenchmark {

    companion object: KLogging()

    @Param("tenant-a", "")
    var tenant: String = ""

    @Param("true", "false")
    var readOnly: Boolean = false

    private lateinit var resolver: ContextAwareRoutingKeyResolver

    @Setup
    fun setup() {
        resolver = ContextAwareRoutingKeyResolver(
            defaultTenant = "default",
            tenantSupplier = { tenant.ifBlank { null } },
            readOnlySupplier = { readOnly }
        )
    }

    @Benchmark
    fun currentLookupKey(): String = resolver.currentLookupKey()
}
