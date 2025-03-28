package alternative.r2dbc.example.config

import alternative.r2dbc.example.domain.repository.CustomerRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.r2dbc.connection.init.connectionFactoryInitializer
import io.bluetape4k.r2dbc.connection.init.resourceDatabasePopulatorOf
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
// @ComponentScan(basePackageClasses =  [CustomerRepository::class])
@EnableR2dbcRepositories(basePackageClasses = [CustomerRepository::class])
class R2dbcConfig: AbstractR2dbcConfiguration() {

    companion object: KLogging()

    @Bean("connectionUrl")
    @Profile("h2")
    fun connectionUrlH2(): String {
        return "r2dbc:h2:mem:///test?MODE=PostgreSQL;options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
    }

    @Bean
    @Profile("h2")
    fun resourceDatabasePopulatorH2(): CompositeDatabasePopulator {
        return CompositeDatabasePopulator().apply {
            addPopulators(resourceDatabasePopulatorOf(ClassPathResource("data/schema-h2.sql")))
        }
    }

    @Bean("connectionUrl")
    @Profile("postgres")
    fun connectionUrlPostgres(): String {
        val postgres = PostgreSQLServer.Launcher.postgres
        return postgres.getJdbcUrl().replace("jdbc:", "r2dbc:")
    }

    @Bean
    @Profile("postgres")
    fun resourceDatabasePopulator(): CompositeDatabasePopulator {
        return CompositeDatabasePopulator().apply {
            addPopulators(resourceDatabasePopulatorOf(ClassPathResource("data/schema-postgres.sql")))
        }
    }

    @Value("\${spring.profiles.active:postgres}")
    private val activeProfiles: String = "h2"

    override fun connectionFactory(): ConnectionFactory {
        val connectionUrl = when {
            activeProfiles.contains("h2") -> connectionUrlH2()
            activeProfiles.contains("postgres") -> connectionUrlPostgres()
            else -> throw IllegalStateException("Unknown profile: $activeProfiles")
        }
        return ConnectionFactories.get(connectionUrl)
    }

    @Bean
    fun initializer(cf: ConnectionFactory, populator: CompositeDatabasePopulator): ConnectionFactoryInitializer {
        return connectionFactoryInitializer(cf) {
            setDatabasePopulator(populator)
        }
    }
}
