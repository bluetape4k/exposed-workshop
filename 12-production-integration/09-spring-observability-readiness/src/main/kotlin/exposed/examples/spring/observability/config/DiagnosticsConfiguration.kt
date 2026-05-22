package exposed.examples.spring.observability.config

import javax.sql.DataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
internal class DiagnosticsConfiguration {

    @Bean
    fun database(dataSource: DataSource): Database =
        Database.connect(dataSource)
}
