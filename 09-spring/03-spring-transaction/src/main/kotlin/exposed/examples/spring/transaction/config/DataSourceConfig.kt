package exposed.examples.spring.transaction.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import exposed.examples.spring.transaction.service.OrderService
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.TransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.TransactionManagementConfigurer
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
/**
 * 테스트용 데이터소스와 Exposed `SpringTransactionManager` 를 구성하는 설정입니다.
 */
class DataSourceConfig: TransactionManagementConfigurer {

    companion object: KLogging()

    /**
     * 메모리 H2 기반 데이터소스를 생성합니다.
     */
    @Bean
    fun dataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:${Base58.randomString(8)};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL"
            driverClassName = "org.h2.Driver"
            username = "root"
            password = ""
        }

        return HikariDataSource(config)
    }

    /**
     * `@Transactional` 에서 사용할 Exposed `SpringTransactionManager` 를 등록합니다.
     */
    @Bean
    override fun annotationDrivenTransactionManager(): TransactionManager {
        log.info { "Create Exposed's SpringTransactionManager" }
        return SpringTransactionManager(dataSource(), DatabaseConfig { useNestedTransactions = true })
    }

    /**
     * 주문 도메인 서비스를 등록합니다.
     */
    @Bean
    fun orderService(): OrderService = OrderService()
}
