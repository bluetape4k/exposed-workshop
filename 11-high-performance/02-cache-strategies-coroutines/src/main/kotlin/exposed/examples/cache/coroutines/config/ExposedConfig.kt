package exposed.examples.cache.coroutines.config

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.sql.Connection
import javax.sql.DataSource

/**
 * 코루틴 기반 캐시 예제에서 Exposed [Database]와 트랜잭션 옵션을 등록합니다.
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
class ExposedConfig {

    companion object: KLoggingChannel()

    /**
     * Exposed 전역 동작에 사용할 [DatabaseConfig]를 구성합니다.
     */
    @Bean
    fun databaseConfig(): DatabaseConfig {
        return DatabaseConfig {
            maxEntitiesToStoreInCachePerEntity = 1000
            useNestedTransactions = true
            defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
        }
    }

    /**
     * 애플리케이션 [DataSource]를 Exposed [Database]에 연결합니다.
     */
    @Bean
    fun database(dataSource: DataSource, databaseConfig: DatabaseConfig): Database {
        log.info { "Database connection: $dataSource" }

        return Database.connect(dataSource, databaseConfig = databaseConfig)
    }
}
