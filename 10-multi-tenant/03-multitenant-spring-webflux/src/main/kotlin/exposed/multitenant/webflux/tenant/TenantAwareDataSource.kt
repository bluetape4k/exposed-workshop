package exposed.multitenant.webflux.tenant

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.runBlocking
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource

/**
 * 코루틴 컨텍스트에 저장된 현재 테넌트를 기준으로 대상 데이터소스를 고르는 라우팅 DataSource 입니다.
 */
class TenantAwareDataSource: AbstractRoutingDataSource() {

    companion object: KLoggingChannel()

    override fun determineCurrentLookupKey(): Any? {
        // WARNING: runBlocking은 Netty 이벤트 루프 스레드를 차단할 수 있습니다.
        // 또한 runBlocking은 새 코루틴 스코프를 생성하므로 상위 코루틴 컨텍스트의 TenantId에
        // 접근할 수 없어 항상 DEFAULT_TENANT를 반환할 가능성이 있습니다.
        // 해결: WebFlux에서는 TenantAwareDataSource 대신 newSuspendedTransactionWithCurrentReactorTenant 패턴 사용 권장.
        return runBlocking { currentTenant() }
    }
}
