package exposed.multitenant.springweb.tenant

import org.jetbrains.exposed.v1.core.Schema

/**
 * 현재 요청의 테넌트 정보를 보관하고 조회하는 컨텍스트입니다.
 *
 * Spring MVC 환경에서 `ThreadLocal`을 사용해 요청 단위 테넌트를 유지합니다.
 */
object TenantContext {

    private val currentTenant = ThreadLocal<Tenants.Tenant?>()

    /**
     * 현재 스레드에 테넌트를 설정합니다.
     */
    fun setCurrentTenant(tenant: Tenants.Tenant) = currentTenant.set(tenant)

    /**
     * 현재 스레드의 테넌트를 반환합니다.
     *
     * 값이 없으면 [Tenants.DEFAULT_TENANT]를 반환합니다.
     */
    fun getCurrentTenant(): Tenants.Tenant = currentTenant.get() ?: Tenants.DEFAULT_TENANT

    /**
     * 현재 테넌트에 해당하는 스키마 정의를 반환합니다.
     */
    fun getCurrentTenantSchema(): Schema = getSchemaDefinition(getCurrentTenant())

    /**
     * 현재 스레드에 저장된 테넌트를 제거합니다.
     */
    fun clear() = currentTenant.remove()

    /**
     * 지정한 [tenant]로 컨텍스트를 설정한 뒤 [block]을 실행합니다.
     *
     * [block] 실행 후에는 반드시 컨텍스트를 정리합니다.
     */
    inline fun withTenant(
        tenant: Tenants.Tenant = getCurrentTenant(),
        block: () -> Unit,
    ) {
        setCurrentTenant(tenant)
        try {
            block()
        } finally {
            clear()
        }
    }
}
