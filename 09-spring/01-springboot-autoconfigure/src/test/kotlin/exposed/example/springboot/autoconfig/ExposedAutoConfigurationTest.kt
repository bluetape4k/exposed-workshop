package exposed.example.springboot.autoconfig

import exposed.example.springboot.Application
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.spring.DatabaseInitializer
import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.DatabaseConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import kotlin.test.Ignore
import kotlin.test.assertFailsWith

/**
 * SpringBoot Application 에서 Exposed 의 Custom DatabaseConfig 를 설정하는 방법
 */
@SpringBootTest(
    classes = [Application::class, ExposedAutoConfigurationTest.CustomDatabaseConfigConfiguration::class],
    properties = [
        "spring.datasource.url=jdbc:h2:mem:test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.exposed.generate-ddl=false",    // DDL 자동 생성 여부
        "spring.exposed.show-sql=true"         // SQL 로그 출력 여부
    ]
)
class ExposedAutoConfigurationTest {

    companion object: KLogging()

    @TestConfiguration
    class CustomDatabaseConfigConfiguration {
        companion object {
            val DEFAULT_EXPOSED_DATABASE_CONFIG = DatabaseConfig {
                maxEntitiesToStoreInCachePerEntity = 100
            }
        }

        @Bean
        fun customDatabaseConfig(): DatabaseConfig = DEFAULT_EXPOSED_DATABASE_CONFIG
    }

    /**
     * Exposed 의 [SpringTransactionManager] 빈이 생성되었는지 확인
     */
    @Autowired(required = false)
    private val springTransactionManager: SpringTransactionManager = uninitialized()

    /**
     * Exposed의 [DatabaseInitializer] 빈이 생성되었는지 확인
     */
    @Autowired(required = false)
    private val databaseInitializer: DatabaseInitializer = uninitialized()

    /**
     * Custom Exposed의 [DatabaseConfig] 빈이 생성되었는지 확인
     */
    @Autowired
    private val databaseConfig: DatabaseConfig = uninitialized()

    @Test
    fun `데이터베이스 커넥션이 초기화 되어야 합니다`() {
        springTransactionManager.shouldNotBeNull()
        databaseConfig.shouldNotBeNull()

        // spring.exposed.generate-ddl=false 이므로 
        databaseInitializer.shouldBeNull()
    }

    @Test
    fun `exposed database config 를 재정의할 수 있어야 합니다`() {
        val expectedConfig = CustomDatabaseConfigConfiguration.DEFAULT_EXPOSED_DATABASE_CONFIG

        databaseConfig shouldBeEqualTo expectedConfig

        log.info { "databaseConfig: $databaseConfig" }

        // 100
        databaseConfig.maxEntitiesToStoreInCachePerEntity shouldBeEqualTo
                expectedConfig.maxEntitiesToStoreInCachePerEntity
    }

    /**
     * Application 에서 [DataSourceTransactionManagerAutoConfiguration] 이 제외되었다.
     */
    @Ignore("누구나 다아는 AutoConfiguration 제외하는 기능이라 생략")
    @Test
    fun `auto configuration 로 부터 특정 클래스를 제외할 수 있습니다`() {
        // Application 만 사용하면 DataSourceTransactionManagerAutoConfiguration 빈이 제외되었습니다.
        val contextRunner = ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(Application::class.java)
        )
        contextRunner.run { context ->
            assertFailsWith<NoSuchBeanDefinitionException> {
                context.getBean(DataSourceTransactionManagerAutoConfiguration::class.java)
            }
        }
    }


}
