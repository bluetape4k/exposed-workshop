package exposed.examples.springwebflux.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.testcontainers.database.getDataSource
import org.jetbrains.exposed.sql.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

/**
 * Exposed 관련 설정입니다. 여기서는 예제용으로 프로파일 기준으로 DB를 설정합니다.
 *
 * 일반적으로는 Exposed의 Spring Boot 용 AutoConfiguration을 사용하면 됩니다.
 */
@Configuration
class ExposedDbConfig {

    companion object: KLogging()

    @Bean
    @Profile("h2")
    fun dataSourceH2(): DataSource {
        log.info { "H2 Database Configuration" }

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;"
            driverClassName = "org.h2.Driver"
            username = "sa"
            password = ""
        }
        return HikariDataSource(config)
    }

    @Bean
    @Profile("mysql")
    fun dataSourceMySql(): DataSource {
        log.info { "MySQL Database Configuration" }

        val mysql = MySQL8Server.Launcher.mysql
        return mysql.getDataSource()
    }

    @Bean
    @Profile("postgres")
    fun dataSourcePostgres(): DataSource {
        log.info { "PostgreSQL Database Configuration" }

        val postgres = PostgreSQLServer.Launcher.postgres
        return postgres.getDataSource()
    }

    @Bean
    fun database(dataSource: DataSource): Database {
        log.info { "Database connection: $dataSource" }
        return Database.connect(dataSource)
    }
}
