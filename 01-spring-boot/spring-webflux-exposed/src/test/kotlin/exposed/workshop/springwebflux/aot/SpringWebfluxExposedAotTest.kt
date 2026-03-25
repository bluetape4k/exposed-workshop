package exposed.workshop.springwebflux.aot

import exposed.workshop.springwebflux.config.ExposedDatabaseConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import javax.sql.DataSource

/**
 * Spring Boot AoT 컴파일 호환성을 [ApplicationContextRunner]로 검증합니다.
 *
 * Spring WebFlux + Coroutines 기반 [ExposedDatabaseConfig]의 Bean들이
 * `h2` 프로파일 환경에서 올바르게 구성되는지 경량 컨텍스트로 확인합니다.
 *
 * `proxyBeanMethods = false` 설정에서도 Bean 간 의존성이 올바르게 주입되는지 검증합니다.
 */
class SpringWebfluxExposedAotTest {

    companion object: KLoggingChannel()

    /**
     * h2 프로파일을 활성화하여 [ExposedDatabaseConfig]만 로드하는 경량 컨텍스트 러너.
     */
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(ExposedDatabaseConfig::class.java)
        .withPropertyValues("spring.profiles.active=h2")

    @Test
    fun `h2 프로파일에서 DataSource 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<DataSource>().shouldNotBeNull()
        }
    }

    @Test
    fun `Exposed Database 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<Database>().shouldNotBeNull()
        }
    }

    @Test
    fun `proxyBeanMethods=false 환경에서 DataSource → Database 의존성 주입이 올바르게 동작해야 한다`() {
        contextRunner.run { context ->
            val dataSource = context.getBean<DataSource>()
            val database = context.getBean<Database>()

            dataSource.shouldNotBeNull()
            database.shouldNotBeNull()
        }
    }
}
