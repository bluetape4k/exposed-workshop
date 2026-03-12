package exposed.multitenant.springweb.tenant

import exposed.multitenant.springweb.tenant.Tenants.Tenant
import org.jetbrains.exposed.v1.core.Schema

/**
 * 현재 요청의 테넌트 정보를 [ScopedValue]로 보관하는 컨텍스트입니다.
 */
object TenantContext {

    val CURRENT_TENANT: ScopedValue<Tenant> = ScopedValue.newInstance()

    /**
     * 현재 실행 컨텍스트의 테넌트를 반환합니다.
     */
    fun getCurrentTenant(): Tenants.Tenant = CURRENT_TENANT.get() ?: Tenants.DEFAULT_TENANT

    /**
     * 현재 테넌트에 해당하는 스키마 정의를 반환합니다.
     */
    fun getCurrentTenantSchema(): Schema = getSchemaDefinition(getCurrentTenant())

    /**
     * 지정한 [tenant]를 컨텍스트에 바인딩하고 [block]을 실행합니다.
     */
    inline fun withTenant(
        tenant: Tenants.Tenant = getCurrentTenant(),
        crossinline block: () -> Unit,
    ) {
        ScopedValue.where(CURRENT_TENANT, tenant).run {
            block()
        }
    }
}
