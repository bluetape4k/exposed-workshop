package exposed.examples.cache.aot

import exposed.examples.cache.config.ExposedConfig
import exposed.examples.cache.config.RedissonConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import javax.sql.DataSource

/**
 * Spring Boot AoT 컴파일 호환성을 [ApplicationContextRunner]로 검증합니다.
 *
 * 캐시 전략 모듈의 Bean들이 AoT 환경에서 올바르게 구성되는지 확인합니다:
 * - [ExposedConfig]: Exposed 데이터베이스 구성 (`DatabaseConfig`, `Database` 빈)
 * - [RedissonConfig]: Redisson Redis 클라이언트 구성 (`RedissonClient` 빈)
 */
class CacheStrategyAotTest {

    companion object : KLoggingChannel()

    /**
     * Exposed DB 구성을 검증하는 컨텍스트 러너.
     *
     * [DataSourceAutoConfiguration]이 DataSource를 자동 구성하고,
     * [ExposedConfig]가 [DatabaseConfig]와 [Database] 빈을 생성합니다.
     */
    private val exposedContextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration::class.java))
        .withUserConfiguration(ExposedConfig::class.java)
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:cache-aot-test;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
        )

    /**
     * Redisson 클라이언트 구성을 검증하는 컨텍스트 러너.
     *
     * [RedissonConfig]가 [RedissonClient] 빈을 생성합니다.
     */
    private val redissonContextRunner = ApplicationContextRunner()
        .withUserConfiguration(RedissonConfig::class.java)

    // ─── Exposed DB 구성 테스트 ───────────────────────────────────────────────

    @Test
    fun `DataSource 빈이 자동 구성되어야 한다`() {
        exposedContextRunner.run { context ->
            context.getBean<DataSource>().shouldNotBeNull()
        }
    }

    @Test
    fun `DatabaseConfig 빈이 생성되어야 한다`() {
        exposedContextRunner.run { context ->
            context.getBean<DatabaseConfig>().shouldNotBeNull()
        }
    }

    @Test
    fun `Exposed Database 빈이 생성되어야 한다`() {
        exposedContextRunner.run { context ->
            context.getBean<Database>().shouldNotBeNull()
        }
    }

    // ─── Redisson 클라이언트 구성 테스트 ─────────────────────────────────────

    @Test
    fun `RedissonClient 빈이 생성되어야 한다`() {
        redissonContextRunner.run { context ->
            context.getBean<RedissonClient>().shouldNotBeNull()
        }
    }
}
