package exposed.multitenant.springweb.tenant

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource

class TenantAwareDataSource: AbstractRoutingDataSource() {

    override fun determineCurrentLookupKey(): Any? {
        // 현 Request 에 해당하는 DataSource 를 결정하는 로직
        // X-TENANT-ID 헤더를 읽어서 공통 TenantContext에 binding된 tenant를 기준으로 DataSource를 결정
        return TenantContexts.current()
    }
}
