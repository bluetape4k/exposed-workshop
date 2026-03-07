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
 * Exposed 데이터베이스 연결 설정 클래스.
 *
 * 활성 Spring 프로파일에 따라 적절한 [DataSource]를 구성하고,
 * Exposed [Database] 인스턴스를 빈으로 등록합니다.
 *
 * 지원 프로파일:
 * - `h2`: 인메모리 H2 데이터베이스 (PostgreSQL 호환 모드)
 * - `mysql`: Testcontainers 기반 MySQL 8 데이터베이스
 * - `postgres`: Testcontainers 기반 PostgreSQL 데이터베이스
 *
 * Spring 트랜잭션 관리(@EnableTransactionManagement)를 활성화하여
 * @Transactional 어노테이션을 통한 선언적 트랜잭션 처리를 지원합니다.
 */
@Configuration
@EnableTransactionManagement
class ExposedDatabaseConfig {

    companion object: KLogging()

    /**
     * H2 인메모리 데이터베이스용 [DataSource]를 생성합니다.
     *
     * `h2` 프로파일이 활성화된 경우에만 빈으로 등록됩니다.
     * PostgreSQL 호환 모드로 실행되어 PostgreSQL 문법을 사용할 수 있습니다.
     *
     * @return HikariCP 커넥션 풀로 감싼 H2 [DataSource]
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
        }
        return HikariDataSource(config)
    }

    /**
     * Testcontainers로 구동되는 MySQL 8 데이터베이스용 [DataSource]를 생성합니다.
     *
     * `mysql` 프로파일이 활성화된 경우에만 빈으로 등록됩니다.
     *
     * @return MySQL 8 컨테이너에 연결된 [DataSource]
     */
    @Bean
    @Profile("mysql")
    fun dataSourceMySql(): DataSource {
        log.info { "MySQL Database Configuration" }

        val mysql = MySQL8Server.Launcher.mysql
        return mysql.getDataSource()
    }

    /**
     * Testcontainers로 구동되는 PostgreSQL 데이터베이스용 [DataSource]를 생성합니다.
     *
     * `postgres` 프로파일이 활성화된 경우에만 빈으로 등록됩니다.
     *
     * @return PostgreSQL 컨테이너에 연결된 [DataSource]
     */
    @Bean
    @Profile("postgres")
    fun dataSourcePostgres(): DataSource {
        log.info { "PostgreSQL Database Configuration" }

        val postgres = PostgreSQLServer.Launcher.postgres
        return postgres.getDataSource()
    }

    /**
     * Exposed [Database] 인스턴스를 생성하여 Spring 빈으로 등록합니다.
     *
     * 주입된 [DataSource]를 사용하여 Exposed 프레임워크의 데이터베이스 연결을 초기화합니다.
     *
     * @param dataSource 데이터베이스 연결에 사용할 [DataSource]
     * @return Exposed [Database] 인스턴스
     */
    @Bean
    fun database(dataSource: DataSource): Database {
        log.info { "Create Database instance with dataSource: $dataSource" }
        return Database.connect(dataSource)
    }
}
