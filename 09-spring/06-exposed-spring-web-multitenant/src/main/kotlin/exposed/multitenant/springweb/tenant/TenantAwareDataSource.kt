package exposed.multitenant.springweb.tenant

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource

class TenantAwareDataSource: AbstractRoutingDataSource() {

    override fun determineCurrentLookupKey(): Any? {
        // 현 Request 에 해당하는 DataSource 를 결정하는 로직
        // X-TENANT-ID 헤더를 읽어서 TenantContext 에 저장된 Tenant ID 를 기준으로 DataSource 를 결정
        return TenantContext.getCurrentTenant()
    }
}
