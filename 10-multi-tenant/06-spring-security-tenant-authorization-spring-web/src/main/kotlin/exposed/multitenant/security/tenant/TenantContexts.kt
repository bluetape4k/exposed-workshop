package exposed.multitenant.security.tenant

import io.bluetape4k.tenant.ThreadLocalTenantContext
import io.bluetape4k.tenant.TenantId as BluetapeTenantId

/**
 * 애플리케이션 tenant와 공통 `ThreadLocalTenantContext` 사이의 얇은 경계입니다.
 *
 * header parsing, authorization과 database routing은 애플리케이션이 소유하고, 동일
 * Servlet thread의 lexical binding과 cleanup은 공통 artifact에 위임합니다.
 */
object TenantContexts {

    private val delegate = ThreadLocalTenantContext()

    fun currentOrNull(): TenantId? =
        delegate.currentOrNull()?.let { TenantId.fromHeader(it.value) }

    fun current(): TenantId =
        TenantId.fromHeader(delegate.requireCurrent().value)

    fun <T> withTenant(tenantId: TenantId, block: () -> T): T =
        delegate.withTenant(BluetapeTenantId(tenantId.headerValue), block)
}
