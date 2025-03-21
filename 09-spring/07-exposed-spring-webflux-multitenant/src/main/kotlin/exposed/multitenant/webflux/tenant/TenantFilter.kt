package exposed.multitenant.webflux.tenant

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class TenantFilter: WebFilter {

    companion object: KLogging() {
        const val TENANT_HEADER = "X-TENANT-ID"
    }


    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> = mono {
        val tenantId = exchange.request.headers.getFirst(TENANT_HEADER) ?: Tenants.DEFAULT_TENANT.id
        val tenant = Tenants.getById(tenantId)

        chain
            .filter(exchange)
            .contextWrite { it.put("TenantId", TenantId(tenant)) }
            .awaitSingle()
    }
}
