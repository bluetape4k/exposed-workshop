package exposed.multitenant.springweb.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.apache.coyote.ProtocolHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

/**
 * Java 21 부터 지원하는 Virtual Threads 를 Tomcat 에서 사용하도록 설정
 */
@Configuration
@ConditionalOnProperty("spring.threads.virtual.enabled", havingValue = "true")
class TomcatVirtualThreadConfig {

    companion object: KLogging()

    /**
     * Tomcat ProtocolHandler의 executor 를 Virtual Thread 를 사용하는 Executor를 사용하도록 설정
     */
    @Bean
    fun protocolHandlerVirtualThreadExecutorCustomizer(): TomcatProtocolHandlerCustomizer<*> {
        log.info { "Tomcat이 Virtual Threads 를 사용하도록 설정합니다." }

        return TomcatProtocolHandlerCustomizer<ProtocolHandler> { protocolHandler ->
            protocolHandler.executor = Executors.newVirtualThreadPerTaskExecutor()
        }
    }
}
