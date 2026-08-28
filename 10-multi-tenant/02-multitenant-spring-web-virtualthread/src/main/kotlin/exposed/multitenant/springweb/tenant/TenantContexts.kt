package exposed.multitenant.springweb.tenant

import exposed.multitenant.springweb.tenant.Tenants.Tenant
import io.bluetape4k.tenant.ScopedValueTenantContext
import io.bluetape4k.tenant.TenantId as BluetapeTenantId

/**
 * 애플리케이션 tenant와 공통 `ScopedValueTenantContext` 사이의 얇은 경계입니다.
 *
 * header parsing, tenant 존재 확인과 schema 선택은 애플리케이션이 소유하고, carrier의
 * lexical binding과 no-default 조회 의미는 공통 artifact에 위임합니다.
 */
object TenantContexts {

    private val delegate = ScopedValueTenantContext()

    fun currentOrNull(): Tenant? =
        delegate.currentOrNull()?.let { Tenants.getById(it.value) }

    fun current(): Tenant =
        Tenants.getById(delegate.requireCurrent().value)

    fun <T> withTenant(tenant: Tenant, block: () -> T): T =
        delegate.withTenant(BluetapeTenantId(tenant.id), block)
}
