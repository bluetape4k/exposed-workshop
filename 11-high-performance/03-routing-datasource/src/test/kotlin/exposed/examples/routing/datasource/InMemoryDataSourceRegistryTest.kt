package exposed.examples.routing.datasource

import org.h2.Driver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import java.util.concurrent.Executors
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

        assertEquals(count, registry.keys().size)
        assertTrue(registry.contains("tenant-1:rw"))
        assertNotNull(registry.get("tenant-1:rw"))
    }

    @Test
    fun `중복 키 등록 시 새 DataSource로 덮어쓴다`() {
        val registry = InMemoryDataSourceRegistry()
        val original = testDataSource("original")
        val replacement = testDataSource("replacement")

        registry.register("tenant:rw", original)
        registry.register("tenant:rw", replacement)

        assertEquals(1, registry.keys().size)
        assertEquals(replacement, registry.get("tenant:rw"))
    }

    @Test
    fun `존재하지 않는 키 조회 시 null을 반환한다`() {
        val registry = InMemoryDataSourceRegistry()
        assertNull(registry.get("nonexistent:rw"))
    }

    private fun testDataSource(name: String) =
        SimpleDriverDataSource(
            Driver(),
            "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            "",
        )
}

