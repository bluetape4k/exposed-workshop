package exposed.examples.springcache.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class ExposedConfig {

    companion object: KLogging()

    @Bean
    fun databaseConfig(): DatabaseConfig {
        return DatabaseConfig {
            maxEntitiesToStoreInCachePerEntity = 100
            useNestedTransactions = true
        }
    }

    @Bean
    fun database(dataSource: DataSource, databaseConfig: DatabaseConfig): Database {
        log.info { "Database connection: $dataSource" }

        return Database.connect(dataSource, databaseConfig = databaseConfig)
    }
}
