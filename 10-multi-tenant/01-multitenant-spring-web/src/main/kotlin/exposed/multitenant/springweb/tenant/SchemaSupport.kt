package exposed.multitenant.springweb.tenant

import org.jetbrains.exposed.v1.core.Schema

/**
 * 테넌트 정보를 기반으로 Exposed [Schema] 정의를 생성합니다.
 *
 * 테넌트 ID를 스키마 이름으로 사용하며, Oracle 데이터베이스의 테이블스페이스 설정을 포함합니다.
 *
 * @param tenant 스키마를 생성할 테넌트 정보
 * @return 테넌트에 해당하는 Exposed [Schema] 정의
 */
internal fun getSchemaDefinition(tenant: Tenants.Tenant): Schema =
    Schema(
        tenant.id,
        defaultTablespace = "USERS",
        temporaryTablespace = "TEMP ",
        quota = "20M",
        on = "USERS"
    )
