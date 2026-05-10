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
class TransactionSchemaAspect(private val dataSource: DataSource) {
    companion object : KLogging()

    /**
     * `@Transactional` 이 적용된 메소드에 대해 Multitenncy 를 지원하기 위한 Schema 설정을 수행합니다.
     */
    @Before(
        "@within(org.springframework.transaction.annotation.Transactional) || " +
            "@annotation(org.springframework.transaction.annotation.Transactional)"
    )
    fun setSchemaForTransaction() {
        val tenant = TenantContext.getCurrentTenant()
        val schemaName = tenant.id
        log.debug { "Use schema=$schemaName" }

        dataSource.connection.use { connection ->
            connection.autoCommit = true
            connection.createStatement().use { statement ->
                statement.execute("CREATE SCHEMA IF NOT EXISTS $schemaName")
            }
        }

        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            connection.createStatement().use { statement ->
                statement.execute("SET SCHEMA $schemaName")
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }
}
