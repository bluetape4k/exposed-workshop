package exposed.examples.routing.datasource

import org.springframework.jdbc.datasource.AbstractDataSource
import java.sql.Connection

/**
 * [RoutingKeyResolver]가 계산한 키로 [DataSourceRegistry]에서 대상 데이터소스를 찾아 위임합니다.
 */
class DynamicRoutingDataSource(
    private val registry: DataSourceRegistry,
    private val keyResolver: RoutingKeyResolver,
): AbstractDataSource() {

    override fun getConnection(): Connection {
        return resolveCurrentDataSource().connection
    }

    override fun getConnection(username: String?, password: String?): Connection {
        return resolveCurrentDataSource().getConnection(username, password)
    }

    private fun resolveCurrentDataSource() =
        keyResolver.currentLookupKey().let { key ->
            registry.get(key)
                ?: error("No DataSource registered for key=$key. keys=${registry.keys()}")
        }
}
