package exposed.multitenant.springweb.tenant

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class TenantSchemaAspect(private val dataSource: DataSource) {
    companion object: KLogging()

    /**
     * `@Transactional` 이 적용된 클래스나 메소드에 대해 Multitenncy 를 지원하기 위한 Schema 설정을 수행합니다.
     *
     * WARNING: Exposed의 `transaction { SchemaUtils.setSchema(...) }` 방식은 Spring이 관리하는 트랜잭션과
     * 별개의 커넥션을 열어 스키마를 설정하므로, 실제 비즈니스 로직이 실행되는 커넥션에는 적용되지 않습니다.
     * Spring이 관리하는 트랜잭션 커넥션에 직접 스키마를 설정하려면 DataSourceUtils를 사용해야 합니다.
     */
    @Before(
        "@within(org.springframework.transaction.annotation.Transactional) || " +
                "@annotation(org.springframework.transaction.annotation.Transactional)"
    )
    fun setSchemaForTransaction() {
        val tenant = TenantContext.getCurrentTenant()
        val schemaName = tenant.id
        log.debug { "Use schema=$schemaName" }
        // Spring이 관리하는 트랜잭션 커넥션에 스키마를 직접 설정합니다.
        // DataSourceUtils.getConnection()은 현재 트랜잭션에 바인딩된 커넥션을 반환합니다.
        val conn = DataSourceUtils.getConnection(dataSource)
        try {
            conn.schema = schemaName
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource)
        }
    }
}
