package exposed.examples.routing.datasource

import org.h2.Driver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    private fun testDataSource(name: String) =
        SimpleDriverDataSource(
            Driver(),
            "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            "",
        )
}

