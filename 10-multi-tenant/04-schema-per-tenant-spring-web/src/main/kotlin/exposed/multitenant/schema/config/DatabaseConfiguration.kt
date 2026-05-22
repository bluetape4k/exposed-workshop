package exposed.multitenant.schema.config

import com.zaxxer.hikari.HikariDataSource
import exposed.multitenant.schema.tenant.ConnectionEvictor
import exposed.multitenant.schema.tenant.ConnectionUsageProbe
import exposed.multitenant.schema.tenant.ExposedTransactionRollbacker
import exposed.multitenant.schema.tenant.H2SchemaResetter
import exposed.multitenant.schema.tenant.SchemaResetter
import exposed.multitenant.schema.tenant.TransactionRollbacker
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.sql.Connection
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {

    @Bean
    fun database(dataSource: DataSource): Database =
        Database.connect(datasource = dataSource)

    @Bean
    fun schemaResetter(): SchemaResetter = H2SchemaResetter()

    @Bean
    fun transactionRollbacker(): TransactionRollbacker = ExposedTransactionRollbacker()

    @Bean
    fun connectionUsageProbe(): ConnectionUsageProbe =
        ConnectionUsageProbe { _, _ -> }

    @Bean
    fun connectionEvictor(dataSource: DataSource): ConnectionEvictor =
        ConnectionEvictor { connection: Connection ->
            (dataSource as? HikariDataSource)?.evictConnection(connection)
                ?: connection.close()
        }
}
