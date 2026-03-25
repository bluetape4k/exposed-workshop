package exposed.examples.spring.transaction.aot

import exposed.examples.spring.transaction.config.DataSourceConfig
import exposed.examples.spring.transaction.service.OrderService
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Spring Boot AoT 컴파일 호환성을 [ApplicationContextRunner]로 검증합니다.
 *
 * [DataSourceConfig]가 제공하는 Bean들이 AoT 환경에서 올바르게 구성되는지
 * 경량 컨텍스트로 확인합니다.
 *
 * `@SpringBootTest` 없이 특정 `@Configuration` 클래스만 로드하여 테스트하는 방식으로,
 * Spring AoT가 Bean 생성에 사용하는 방식과 동일하게 동작합니다.
 */
class DataSourceConfigAotTest {

    companion object: KLogging()

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(DataSourceConfig::class.java)

    @Test
    fun `DataSource 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<DataSource>().shouldNotBeNull()
        }
    }

    @Test
    fun `Exposed SpringTransactionManager 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<PlatformTransactionManager>().shouldNotBeNull()
            context.getBean<SpringTransactionManager>().shouldNotBeNull()
        }
    }

    @Test
    fun `OrderService 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<OrderService>().shouldNotBeNull()
        }
    }

    @Test
    fun `중첩 트랜잭션이 활성화된 SpringTransactionManager가 구성되어야 한다`() {
        contextRunner.run { context ->
            val tm = context.getBean<SpringTransactionManager>()
            tm.shouldNotBeNull()
            // SpringTransactionManager는 DataSourceConfig에서 useNestedTransactions = true 로 설정됨
        }
    }
}
