package exposed.multitenant.webflux.tenant

import io.bluetape4k.coroutines.reactor.currentReactiveContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

data class TenantId(val value: Tenants.Tenant): CoroutineContext.Element {
    companion object Key: CoroutineContext.Key<TenantId> {
        val DEFAULT = TenantId(Tenants.DEFAULT_TENANT)
    }

    override val key: CoroutineContext.Key<*> = Key

    override fun toString(): String {
        return "TenantId(value='$value')"
    }
}

suspend fun currentReactorTenant(): Tenants.Tenant =
    currentReactiveContext()?.getOrDefault("TenantId", TenantId.DEFAULT)?.value
        ?: Tenants.DEFAULT_TENANT


suspend fun currentTenant(): Tenants.Tenant =
    coroutineContext[TenantId]?.value ?: Tenants.DEFAULT_TENANT
