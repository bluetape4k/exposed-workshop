package exposed.examples.springwebflux.aot

import exposed.examples.springwebflux.config.ExposedDbConfig
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
 * Spring WebFlux + Coroutines 기반 [ExposedDbConfig]의 Bean들이
 * `h2` 프로파일 환경에서 올바르게 구성되는지 경량 컨텍스트로 확인합니다.
 *
 * `@SpringBootTest` 없이 특정 `@Configuration` 클래스만 로드하여 테스트합니다.
 * Spring AoT가 Bean 생성에 사용하는 방식과 동일하게 동작합니다.
 */
class CoroutineExposedRepositoryAotTest {

    companion object: KLoggingChannel()

    /**
     * h2 프로파일을 활성화하여 [ExposedDbConfig]만 로드하는 경량 컨텍스트 러너.
     *
     * `spring.profiles.active=h2` 설정으로 [ExposedDbConfig.dataSourceH2] 빈이 선택됩니다.
     */
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(ExposedDbConfig::class.java)
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
            // proxyBeanMethods=false 이므로 @Bean 메서드를 직접 호출하지 않고
            // Spring 컨테이너가 파라미터로 주입하여 의존성 해결
            val dataSource = context.getBean<DataSource>()
            val database = context.getBean<Database>()

            dataSource.shouldNotBeNull()
            database.shouldNotBeNull()
        }
    }
}
