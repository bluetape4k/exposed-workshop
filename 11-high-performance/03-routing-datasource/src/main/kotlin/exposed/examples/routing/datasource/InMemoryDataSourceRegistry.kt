package exposed.examples.routing.datasource

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
}

