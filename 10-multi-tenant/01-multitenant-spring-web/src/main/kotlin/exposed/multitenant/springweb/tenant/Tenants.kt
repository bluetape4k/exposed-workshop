package exposed.multitenant.springweb.tenant

import org.jetbrains.exposed.v1.core.Schema

/**
 * 멀티테넌트 식별자와 스키마 매핑 정보를 관리합니다.
 */
object Tenants {

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
     * [tenantId]에 해당하는 [Tenant]를 반환합니다.
     *
     * 존재하지 않으면 예외를 발생시킵니다.
     */
    fun getById(tenantId: String): Tenant =
        Tenant.fromId(tenantId) ?: throw IllegalArgumentException("No tenant found for id: $tenantId")

    private val tenantSchemas = mapOf(
        Tenant.KOREAN to getSchemaDefinition(Tenant.KOREAN),
        Tenant.ENGLISH to getSchemaDefinition(Tenant.ENGLISH),
    )

    /**
     * [tenant]에 대응하는 스키마를 반환합니다.
     */
    fun getTenantSchema(tenant: Tenant): Schema =
        tenantSchemas[tenant] ?: error("No schema found for tenant: ${tenant.id}")
}
