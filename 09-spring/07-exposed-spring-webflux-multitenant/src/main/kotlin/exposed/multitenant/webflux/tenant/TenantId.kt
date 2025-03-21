package exposed.multitenant.webflux.tenant

import kotlinx.coroutines.reactor.ReactorContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * 코루틴의 [CoroutineContext] 에 [TenantId] 를 설정합니다.
 */
data class TenantId(val value: Tenants.Tenant): CoroutineContext.Element {
    companion object Key: CoroutineContext.Key<TenantId> {
        val DEFAULT = TenantId(Tenants.DEFAULT_TENANT)

        const val TENANT_ID_KEY = "TenantId"
    }

    override val key: CoroutineContext.Key<*> = Key

    override fun toString(): String {
        return "TenantId(value='$value')"
    }
}

/**
 * [ReactorContext] 에서 `TenantId` 의 정보를 읽어옵니다. 없으면 [TenantId.DEFAULT] 를 사용합니다.
 *
 * Webflux 에서 사용되는 코루틴의 [CoroutineContext] 에서 [TenantId] 를 읽어옵니다.
 */
suspend fun currentReactorTenant(): Tenants.Tenant =
    coroutineContext[ReactorContext]?.context?.getOrDefault(TenantId.TENANT_ID_KEY, TenantId.DEFAULT)?.value
        ?: Tenants.DEFAULT_TENANT


/**
 * 현재 코루틴의 [TenantId] 를 읽어옵니다. 없으면 [Tenants.DEFAULT_TENANT] 를 사용합니다.
 */
suspend fun currentTenant(): Tenants.Tenant =
    coroutineContext[TenantId]?.value ?: Tenants.DEFAULT_TENANT
