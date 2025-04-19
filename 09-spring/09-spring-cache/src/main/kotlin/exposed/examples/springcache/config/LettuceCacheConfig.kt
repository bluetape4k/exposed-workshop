package exposed.examples.springcache.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.spring.serializer.RedisBinarySerializers
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * Spring Cache를 Lettuce를 이용하여 Redis에 저장하기 위한 설정
 */
@Configuration
@EnableCaching
class LettuceCacheConfig {

    companion object: KLogging() {
        private val defaultRedisSerializer = RedisBinarySerializers.LZ4Fury
    }

    @Value("\${spring.data.redis.host}")
    var redisHost: String = "localhost"

    @Value("\${spring.data.redis.port}")
    var redisPort: Int = 6379

    @Bean
    fun redisCacheConfiguration(): RedisCacheConfiguration {
        val serializationPair = RedisSerializationContext.SerializationPair
            .fromSerializer(defaultRedisSerializer)

        return RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(serializationPair)
            .entryTtl(Duration.ofMinutes(10))
    }

    @Bean
    fun cacheManager(
        connectionFactory: RedisConnectionFactory,
        cacheConfiguration: RedisCacheConfiguration,
    ): CacheManager {
        return RedisCacheManager.builder(connectionFactory)
            .transactionAware()
            .cacheDefaults(cacheConfiguration)
            .build()
    }

    @Bean
    @ConditionalOnMissingBean(name = ["redisTemplate"])
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<Any, Any> {
        return RedisTemplate<Any, Any>().apply {
            setConnectionFactory(connectionFactory)
            setDefaultSerializer(defaultRedisSerializer)
            keySerializer = StringRedisSerializer.UTF_8
            valueSerializer = defaultRedisSerializer
        }
    }
}
