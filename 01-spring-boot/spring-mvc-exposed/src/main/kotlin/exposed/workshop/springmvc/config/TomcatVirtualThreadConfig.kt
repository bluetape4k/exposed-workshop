package exposed.workshop.springmvc.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.apache.coyote.ProtocolHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

/**
 * Tomcat이 Virtual Thread를 사용하도록 설정하는 구성 클래스.
 *
 * `app.virtualthread.enabled=true`(기본값)일 때 활성화됩니다.
 */
@Configuration
@ConditionalOnProperty("app.virtualthread.enabled", havingValue = "true", matchIfMissing = true)
class TomcatVirtualThreadConfig {

    companion object: KLogging()

    /**
     * Tomcat ProtocolHandler의 executor 를 Virtual Thread 를 사용하는 Executor를 사용하도록 설정
     */
    @Bean
    fun protocolHandlerVirtualThreadExecutorCustomizer(): TomcatProtocolHandlerCustomizer<*> {
        log.info { "Tomcat 이 Virtual Threads 를 사용하도록 설정합니다." }

        return TomcatProtocolHandlerCustomizer<ProtocolHandler> { protocolHandler ->
            protocolHandler.executor = Executors.newVirtualThreadPerTaskExecutor()
        }
    }
}
