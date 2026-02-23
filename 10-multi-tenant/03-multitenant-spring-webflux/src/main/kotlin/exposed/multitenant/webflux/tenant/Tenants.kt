package exposed.multitenant.webflux.tenant

import org.jetbrains.exposed.v1.core.Schema

/**
 * 테넌트에 대한 정보를 관리합니다.
 */
object Tenants {

    /**
     * 요청 헤더가 없을 때 사용하는 기본 테넌트입니다.
     */
    val DEFAULT_TENANT = Tenant.KOREAN

    /**
     * 지원하는 테넌트 목록입니다.
     */
    enum class Tenant(val id: String) {
        KOREAN("korean"),
        ENGLISH("english");

        companion object {
            /**
             * 문자열 식별자로 [Tenant]를 조회합니다.
             */
            fun fromId(id: String): Tenant? = entries.find { it.id == id }
        }
    }

    /**
     * 식별자에 해당하는 [Tenant]를 반환합니다.
     *
     * 존재하지 않으면 예외를 발생시킵니다.
     */
    fun getById(tenantId: String): Tenant =
        Tenant.fromId(tenantId) ?: error("No tenant found for id: $tenantId")

    private val tenantSchemas = mapOf(
        Tenant.KOREAN to getSchemaDefinition(Tenant.KOREAN),
        Tenant.ENGLISH to getSchemaDefinition(Tenant.ENGLISH),
    )

    /**
     * 지정한 테넌트의 스키마를 반환합니다.
     */
    fun getTenantSchema(tenant: Tenant): Schema =
        tenantSchemas[tenant] ?: error("No schema found for tenant: ${tenant.id}")
}
