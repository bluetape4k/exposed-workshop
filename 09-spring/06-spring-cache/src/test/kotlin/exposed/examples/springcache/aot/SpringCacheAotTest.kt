package exposed.examples.springcache.aot

import exposed.examples.springcache.SpringCacheApplication
import exposed.examples.springcache.config.ExposedConfig
import exposed.examples.springcache.config.LettuceCacheConfig
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cache.CacheManager
import org.springframework.data.redis.cache.RedisCacheConfiguration
import javax.sql.DataSource

/**
 * Spring Boot AoT 컴파일 호환성을 [ApplicationContextRunner]로 검증합니다.
 *
 * Spring Cache + Redis 기반 구성이 AoT 환경에서 올바르게 동작하는지 확인합니다:
 * - [ExposedConfig]: Exposed 데이터베이스 구성 (`DatabaseConfig`, `Database` 빈)
 * - [LettuceCacheConfig]: Redis 캐시 구성 (`RedisCacheConfiguration`, `CacheManager` 빈)
 */
class SpringCacheAotTest {

    companion object: KLogging() {
        // Testcontainers Redis 싱글턴 (첫 접근 시 컨테이너 시작)
        private val redisServer = SpringCacheApplication.redisServer
    }

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
            "spring.datasource.url=jdbc:h2:mem:sc-aot-test;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
        )

    /**
     * Redis 캐시 구성을 검증하는 컨텍스트 러너.
     *
     * [RedisAutoConfiguration]이 [RedisConnectionFactory]를 자동 구성하고,
     * [LettuceCacheConfig]가 [RedisCacheConfiguration]과 [CacheManager] 빈을 생성합니다.
     */
    private val cacheContextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration::class.java))
        .withUserConfiguration(LettuceCacheConfig::class.java)
        .withPropertyValues("spring.data.redis.url=${redisServer.url}")

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

    // ─── Redis 캐시 구성 테스트 ───────────────────────────────────────────────

    @Test
    fun `RedisCacheConfiguration 빈이 생성되어야 한다`() {
        cacheContextRunner.run { context ->
            context.getBean<RedisCacheConfiguration>().shouldNotBeNull()
        }
    }

    @Test
    fun `Redis 기반 CacheManager 빈이 생성되어야 한다`() {
        cacheContextRunner.run { context ->
            context.getBean<CacheManager>().shouldNotBeNull()
        }
    }
}
