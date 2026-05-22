package exposed.examples.routing.datasource

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.h2.Driver
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

/** 동시 등록과 조회를 포함한 `InMemoryDataSourceRegistry`의 스레드 안전성과 기본 동작을 검증합니다. */
class InMemoryDataSourceRegistryTest {

    @Test
    fun `동시 등록 시에도 모든 키가 정상 저장된다`() {
        val registry = InMemoryDataSourceRegistry()
        val executor = Executors.newFixedThreadPool(8)
        val count = 50

        repeat(count) { index ->
            executor.submit {
                registry.register("tenant-$index:rw", testDataSource("tenant-$index-rw"))
            }
        }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        registry.keys().size shouldBeEqualTo count
        registry.contains("tenant-1:rw").shouldBeTrue()
        registry.get("tenant-1:rw").shouldNotBeNull()
    }

    @Test
    fun `중복 키 등록 시 새 DataSource로 덮어쓴다`() {
        val registry = InMemoryDataSourceRegistry()
        val original = testDataSource("original")
        val replacement = testDataSource("replacement")

        registry.register("tenant:rw", original)
        registry.register("tenant:rw", replacement)

        registry.keys().size shouldBeEqualTo 1
        registry.get("tenant:rw") shouldBeSameInstanceAs replacement
    }

    @Test
    fun `존재하지 않는 키 조회 시 null을 반환한다`() {
        val registry = InMemoryDataSourceRegistry()
        registry.get("nonexistent:rw").shouldBeNull()
    }

    @Test
    fun `close는 등록된 closeable DataSource를 한 번씩 닫고 registry를 비운다`() {
        val registry = InMemoryDataSourceRegistry()
        val shared = CloseTrackingDataSource("shared")
        val dedicated = CloseTrackingDataSource("dedicated")

        registry.register("tenant:rw", shared)
        registry.register("tenant:ro", shared)
        registry.register("other:rw", dedicated)

        registry.close()

        shared.closeCount.get() shouldBeEqualTo 1
        dedicated.closeCount.get() shouldBeEqualTo 1
        registry.keys().size shouldBeEqualTo 0
    }

    @Test
    fun `close는 여러 번 호출해도 이미 닫은 DataSource를 다시 닫지 않는다`() {
        val registry = InMemoryDataSourceRegistry()
        val dataSource = CloseTrackingDataSource("idempotent")

        registry.register("tenant:rw", dataSource)

        registry.close()
        registry.close()

        dataSource.closeCount.get() shouldBeEqualTo 1
        registry.keys().size shouldBeEqualTo 0
    }

    @Test
    fun `close 실패 시 다른 DataSource 종료를 계속 시도하고 suppressed exception을 전달한다`() {
        val registry = InMemoryDataSourceRegistry()
        val failing = FailingCloseDataSource("failing")
        val dedicated = CloseTrackingDataSource("dedicated-after-failure")

        registry.register("failing:rw", failing)
        registry.register("dedicated:rw", dedicated)

        val failure = assertFailsWith<IllegalStateException> {
            registry.close()
        }

        failure.suppressed.size shouldBeEqualTo 1
        failure.suppressed.single().message shouldBeEqualTo("close failed: failing")
        failing.closeCount.get() shouldBeEqualTo 1
        dedicated.closeCount.get() shouldBeEqualTo 1
        registry.keys().size shouldBeEqualTo 0
    }

    private fun testDataSource(name: String) =
        SimpleDriverDataSource(
            Driver(),
            "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            "",
        )

    private open class CloseTrackingDataSource(
        protected val dataSourceName: String,
    ): SimpleDriverDataSource(
        Driver(),
        "jdbc:h2:mem:$dataSourceName;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "sa",
        "",
    ), AutoCloseable {
        val closeCount = AtomicInteger()

        override fun close() {
            closeCount.incrementAndGet()
        }
    }

    private class FailingCloseDataSource(name: String): CloseTrackingDataSource(name) {
        override fun close() {
            super.close()
            throw IllegalStateException("close failed: $dataSourceName")
        }
    }
}
