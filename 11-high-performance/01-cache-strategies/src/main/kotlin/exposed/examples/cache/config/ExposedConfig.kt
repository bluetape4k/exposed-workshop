package exposed.examples.cache.config

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
 * Exposed 기본 설정(트ラン잭션 속성, 캐시 제한 등)과 Database 등록을 담당합니다.
 */
@Configuration
@EnableTransactionManagement
class ExposedConfig {

    companion object: KLoggingChannel()

    @Bean
    fun databaseConfig(): DatabaseConfig {
        return DatabaseConfig {
            maxEntitiesToStoreInCachePerEntity = 1000
            useNestedTransactions = true
            defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
        }
    }

    @Bean
    fun database(dataSource: DataSource, databaseConfig: DatabaseConfig): Database {
        log.info { "Database connection: $dataSource" }

        return Database.connect(dataSource, databaseConfig = databaseConfig)
    }
}
