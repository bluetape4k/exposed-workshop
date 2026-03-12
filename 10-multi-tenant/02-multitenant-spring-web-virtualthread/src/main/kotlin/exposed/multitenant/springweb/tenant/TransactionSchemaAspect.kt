package exposed.multitenant.springweb.tenant

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class TransactionSchemaAspect {

    companion object: KLogging()

    /**
     * `@Transactional` 이 적용된 메소드에 대해 Multitenncy 를 지원하기 위한 Schema 설정을 수행합니다.
     */
    @Before(
        "@within(org.springframework.transaction.annotation.Transactional) || " +
                "@annotation(org.springframework.transaction.annotation.Transactional)"
    )
    fun setSchemaForTransaction() {
        transaction {
            val schema = TenantContext.getCurrentTenantSchema()
            log.info { "Use schema=$schema" }
            SchemaUtils.createSchema(schema)
            SchemaUtils.setSchema(schema)
            commit()
        }
    }
}
