package alternatives.hibernate.reactive.example.config

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.context.annotation.Configuration

@Configuration
class DataSourceConfig {
    companion object: KLoggingChannel()

//    @Bean
//    fun dataSource(): DataSource {
//        log.info { "PostgreSQL Database Configuration" }
//
//        val postgres = PostgreSQLServer.Launcher.postgres
//        return postgres.getDataSource()
//    }
}
