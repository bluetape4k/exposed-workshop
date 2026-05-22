package exposed.examples.routing.datasource

import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

/**
 * [DataSourceRegistry]의 스레드 안전한 인메모리 구현체입니다.
 */
class InMemoryDataSourceRegistry: DataSourceRegistry {
    private val dataSources = ConcurrentHashMap<String, DataSource>()

    override fun register(key: String, dataSource: DataSource) {
        dataSources[key] = dataSource
    }

    override fun get(key: String): DataSource? = dataSources[key]

    override fun contains(key: String): Boolean = dataSources.containsKey(key)

    override fun keys(): Set<String> = dataSources.keys

    override fun close() {
        val closeableDataSources = dataSources.values
            .filterIsInstance<AutoCloseable>()
            .distinctByIdentity()
        val failures = mutableListOf<Exception>()

        dataSources.clear()

        closeableDataSources.forEach { dataSource ->
            try {
                dataSource.close()
            } catch (e: Exception) {
                failures.add(e)
            }
        }

        if (failures.isNotEmpty()) {
            throw IllegalStateException("Failed to close registered DataSources.").apply {
                failures.forEach(::addSuppressed)
            }
        }
    }
}

private fun <T: Any> Iterable<T>.distinctByIdentity(): List<T> {
    val seen = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
    return filter { seen.add(it) }
}
