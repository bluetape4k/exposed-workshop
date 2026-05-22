package exposed.examples.spring.auth.config

import javax.sql.DataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
internal class DatabaseConfiguration {

    @Bean
    fun database(dataSource: DataSource): Database =
        Database.connect(dataSource)
}
