package exposed.workshop.springwebflux.config

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Runtimex
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ReactorResourceFactory
import reactor.netty.http.server.HttpServer
import reactor.netty.resources.ConnectionProvider
import reactor.netty.resources.LoopResources
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Webflux 에서 사용하는 Netty 관련 설정을 제공합니다.
 */
@Configuration
class NettyConfig {
    companion object: KLoggingChannel()

    /**
     * 커스텀 이벤트 루프 설정이 적용된 Netty 기반 Reactive 웹 서버 팩토리를 생성합니다.
     *
     * @return 커스터마이저가 적용된 [NettyReactiveWebServerFactory]
     */
    @Bean
    fun nettyReactiveWebServerFactory(): NettyReactiveWebServerFactory {
        return NettyReactiveWebServerFactory().apply {
            addServerCustomizers(EventLoopNettyCustomer())
        }
    }

    /**
     * Netty HTTP 서버에 Keep-Alive, 백로그, 읽기/쓰기 타임아웃을 설정하는 커스터마이저.
     */
    class EventLoopNettyCustomer: NettyServerCustomizer {
        override fun apply(httpServer: HttpServer): HttpServer {
            return httpServer
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_BACKLOG, 8_000)
                .doOnConnection { conn ->
                    conn.addHandlerLast(ReadTimeoutHandler(10))
                    conn.addHandlerLast(WriteTimeoutHandler(10))
                }
        }
    }

    /**
     * Reactor Netty의 연결 풀과 이벤트 루프 리소스를 설정합니다.
     *
     * @return 커스텀 연결 풀과 루프 리소스가 적용된 [ReactorResourceFactory]
     */
    @Bean
    fun reactorResourceFactory(): ReactorResourceFactory {
        return ReactorResourceFactory().apply {
            isUseGlobalResources = false
            connectionProvider = ConnectionProvider.builder("http")
                .maxConnections(8_000)
                .maxIdleTime(30.seconds.toJavaDuration())
                .build()

            loopResources = LoopResources.create(
                "event-loop",
                4,
                maxOf(Runtimex.availableProcessors * 8, 64),
                true
            )
        }
    }
}
