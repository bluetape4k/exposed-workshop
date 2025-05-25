package exposed.examples.suspendedcache.config

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class ExposedConfig {

    companion object: KLoggingChannel()

    @Bean
    fun databaseConfig(): DatabaseConfig {
        return DatabaseConfig {
            maxEntitiesToStoreInCachePerEntity = 500
        }
    }

    @Bean
    fun database(dataSource: DataSource, databaseConfig: DatabaseConfig): Database {
        log.info { "Database connection: $dataSource" }

        return Database.connect(dataSource, databaseConfig = databaseConfig)
    }
}
