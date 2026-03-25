package exposed.workshop.springmvc.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.testcontainers.database.getDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

/**
 * Exposed ORM을 위한 데이터베이스 연결 설정.
 *
 * H2, MySQL, PostgreSQL 프로파일에 따라 적절한 DataSource를 생성하고,
 * Exposed의 [Database] 인스턴스를 Spring 빈으로 등록합니다.
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
class ExposedDatabaseConfig {

    companion object: KLogging()

    /**
     * H2 인메모리 데이터베이스 DataSource를 생성합니다. (`h2` 프로파일 활성 시)
     *
     * @return HikariCP 기반 H2 DataSource
     */
    @Bean
    @Profile("h2")
    fun dataSourceH2(): DataSource {
        log.info { "H2 Database Configuration" }

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;"
            driverClassName = "org.h2.Driver"
            username = "sa"
            password = ""
            maximumPoolSize = maxOf(50, Runtime.getRuntime().availableProcessors() * 4)
        }
        return HikariDataSource(config)
    }

    /**
     * MySQL 8 데이터베이스 DataSource를 생성합니다. (`mysql` 프로파일 활성 시)
     *
     * TestContainers를 사용하여 MySQL 서버를 시작합니다.
     *
     * @return MySQL 기반 DataSource
     */
    @Bean
    @Profile("mysql")
    fun dataSourceMySql(): DataSource {
        log.info { "MySQL Database Configuration" }

        val mysql = MySQL8Server.Launcher.mysql
        return mysql.getDataSource()
    }

    /**
     * PostgreSQL 데이터베이스 DataSource를 생성합니다. (`postgres` 프로파일 활성 시)
     *
     * TestContainers를 사용하여 PostgreSQL 서버를 시작합니다.
     *
     * @return PostgreSQL 기반 DataSource
     */
    @Bean
    @Profile("postgres")
    fun dataSourcePostgres(): DataSource {
        log.info { "PostgreSQL Database Configuration" }

        val postgres = PostgreSQLServer.Launcher.postgres
        return postgres.getDataSource()
    }

    /**
     * DataSource를 사용하여 Exposed [Database] 인스턴스를 생성합니다.
     *
     * @param dataSource 연결에 사용할 DataSource
     * @return Exposed Database 인스턴스
     */
    @Bean
    fun database(dataSource: DataSource): Database {
        log.info { "Create Database instance with dataSource: $dataSource" }
        return Database.connect(dataSource)
    }
}
