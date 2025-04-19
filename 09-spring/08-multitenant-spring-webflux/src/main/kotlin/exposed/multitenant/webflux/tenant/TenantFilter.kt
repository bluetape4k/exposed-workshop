package exposed.multitenant.webflux.tenant

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * 요청 헤더에서 `X-TENANT-ID` 를 읽어서 ReactorContext 에 TenantId 를 설정합니다.
 * 이를 사용하여, CoroutineScope 에서 TenantId 를 사용할 수 있습니다.
 *
 * ```kotlin
 * val tenantId = currentReactorTenant()
 * ```
 *
 * @see [currentReactorTenant]
 */
@Component
class TenantFilter: WebFilter {

    companion object: KLogging() {
        const val TENANT_HEADER = "X-TENANT-ID"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> = mono {
        val tenantId = exchange.request.headers.getFirst(TENANT_HEADER)
        log.debug { "Request tenantId: $tenantId" }
        val tenant = Tenants.getById(tenantId ?: Tenants.DEFAULT_TENANT.id)

        chain
            .filter(exchange)
            .contextWrite { it.put(TenantId.TENANT_ID_KEY, TenantId(tenant)) }
            .awaitSingleOrNull()     // awaitSingle() 을 사용하면, 전송 후에도 뭔가 처리하느라 예외가 발생함.
    }
}
