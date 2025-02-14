package alternatives.hibernate.reactive.example.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.testcontainers.database.getDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DataSourceConfig {
    companion object: KLogging()

    @Bean
    fun dataSource(): DataSource {
        log.info { "PostgreSQL Database Configuration" }

        val postgres = PostgreSQLServer.Launcher.postgres
        return postgres.getDataSource()
    }
}
